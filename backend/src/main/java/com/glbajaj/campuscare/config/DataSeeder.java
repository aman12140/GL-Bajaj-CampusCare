package com.glbajaj.campuscare.config;

import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.repository.*;
import com.glbajaj.campuscare.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Seeds reference data and (optionally) DEMO data on first start.
 *
 *  Always (if missing) : departments, location types, categories, campus locations, ONE admin account.
 *  Only when app.seed-demo-data=true and the database has no students/issues: demo students, demo staff,
 *  and ~19 demo issues covering every status, priority and department.
 *
 * ALL people, IDs and issues created here are DEMO / SAMPLE data. Room names such as "the computer lab" are placeholders -
 * they are NOT real GL Bajaj room numbers. Admins can edit or deactivate everything from the Admin panel.
 * Passwords are hashed with BCrypt before they are stored.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String ADMIN_EMAIL = "admin@campuscare.local";
    public static final String ADMIN_PASSWORD = "Admin@123";
    public static final String STUDENT_PASSWORD = "Student@123";
    public static final String STAFF_PASSWORD = "Staff@123";

    private final DepartmentRepository departmentRepository;
    private final LocationTypeRepository locationTypeRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final IssueRepository issueRepository;
    private final IssueAssignmentRepository assignmentRepository;
    private final IssueStatusHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder encoder;
    private final boolean seedDemo;

    private final Map<String, Department> depts = new HashMap<>();
    private final Map<String, Category> cats = new HashMap<>();
    private final Map<String, Location> locs = new HashMap<>();

    public DataSeeder(DepartmentRepository departmentRepository, LocationTypeRepository locationTypeRepository,
                      CategoryRepository categoryRepository, LocationRepository locationRepository, UserRepository userRepository,
                      StudentRepository studentRepository, StaffRepository staffRepository, IssueRepository issueRepository,
                      IssueAssignmentRepository assignmentRepository, IssueStatusHistoryRepository historyRepository,
                      NotificationRepository notificationRepository, FeedbackRepository feedbackRepository,
                      PasswordEncoder encoder, @Value("${app.seed-demo-data:false}") boolean seedDemo) {
        this.departmentRepository = departmentRepository;
        this.locationTypeRepository = locationTypeRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
        this.issueRepository = issueRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.feedbackRepository = feedbackRepository;
        this.encoder = encoder;
        this.seedDemo = seedDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            seedDepartments();
            seedLocationTypes();
            seedCategories();
            seedLocations();
            log.info("Seeded departments, location types, categories and campus locations.");
        }
        User admin = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseGet(this::createAdmin);
        if (seedDemo && studentRepository.count() == 0 && issueRepository.count() == 0) {
            loadMaps();
            List<Student> students = seedStudents();
            Map<String, Staff> staff = seedStaff();
            seedIssues(admin, students, staff);
            log.info("Seeded DEMO students, staff and issues. Demo logins: {} / {}, student1@campuscare.local / {}, staff.it@campuscare.local / {}",
                    ADMIN_EMAIL, ADMIN_PASSWORD, STUDENT_PASSWORD, STAFF_PASSWORD);
        }
    }

    // ================================================================= reference data
    private void seedDepartments() {
        String[][] d = {
                {"Electrical & Power", "Facility & Maintenance"}, {"Plumbing & Water", "Facility & Maintenance"},
                {"Civil & General Maintenance", "Facility & Maintenance"}, {"Housekeeping & Sanitation", "Facility & Maintenance"},
                {"AC & Cooling", "Facility & Maintenance"}, {"IT & Wi-Fi Support", "Technology"}, {"Computer Labs", "Technology"},
                {"Classroom Facilities", "Academic Facilities"}, {"Laboratory Facilities", "Academic Facilities"},
                {"Library", "Academic Facilities"}, {"Language Lab", "Academic Facilities"},
                {"Hostel", "Student Facilities"}, {"Mess & Cafeteria", "Student Facilities"}, {"Medical", "Student Facilities"},
                {"Sports & Gym", "Student Facilities"}, {"Transport", "Campus Operations"}, {"Security", "Campus Operations"},
                {"Administration", "Campus Operations"}, {"Other / General", "Campus Operations"}};
        for (String[] row : d) {
            Department dep = new Department();
            dep.setName(row[0]);
            dep.setDescription(row[1]);   // the department group from the specification
            departmentRepository.save(dep);
        }
        loadDepartments();
    }

    private void loadDepartments() { depts.clear(); departmentRepository.findAll().forEach(x -> depts.put(x.getName(), x)); }

    private void seedLocationTypes() {
        for (String n : List.of("Academic Block", "Hostel", "Cafeteria / Mess", "Library", "Sports Area", "Auditorium / Seminar Hall",
                "Laboratory Block", "Administrative Block", "Parking Area", "Ground / Garden", "Medical Room", "Other")) {
            LocationType t = new LocationType();
            t.setName(n);
            locationTypeRepository.save(t);
        }
    }

    /** Sub-category entries are "Name" (uses the parent's department) or "Name=Department". */
    private void seedCategories() {
        loadDepartments();
        cat("IT & Internet", "IT & Wi-Fi Support", "Wi-Fi / Internet", "Computer/System", "Network", "Projector/Smart Board=Classroom Facilities");
        cat("Electrical", "Electrical & Power", "Light", "Fan", "Switch/Socket", "Power Supply", "Other Electrical");
        cat("Plumbing & Water", "Plumbing & Water", "Water Leakage", "Tap", "Washroom", "Drainage", "Water Supply");
        cat("Cleaning & Sanitation", "Housekeeping & Sanitation", "Room/Classroom Cleaning", "Washroom Cleaning", "Garbage", "Pest/General Hygiene");
        cat("Classroom & Lab", "Classroom Facilities", "Furniture=Civil & General Maintenance", "Desk/Chair=Civil & General Maintenance", "Board",
                "Lab Equipment=Laboratory Facilities", "AC/Cooling=AC & Cooling", "Other Classroom/Lab");
        cat("Hostel", "Hostel", "Room Maintenance", "Furniture=Civil & General Maintenance", "Water=Plumbing & Water",
                "Electrical=Electrical & Power", "Cleaning=Housekeeping & Sanitation", "Common Area");
        cat("Library", "Library", "Furniture=Civil & General Maintenance", "Computer/Internet=IT & Wi-Fi Support",
                "Lighting=Electrical & Power", "AC=AC & Cooling", "Other");
        cat("Cafeteria / Mess", "Mess & Cafeteria", "Food-related facility issue", "Water=Plumbing & Water",
                "Cleaning=Housekeeping & Sanitation", "Equipment", "Furniture=Civil & General Maintenance");
        cat("Gym & Sports", "Sports & Gym", "Gym Equipment", "Sports Equipment", "Facility Maintenance=Civil & General Maintenance");
        cat("Security", "Security", "CCTV", "Access/Security", "Gate/Barrier", "Other Security");
        cat("Transport", "Transport", "Bus", "Parking", "Transport Facility");
        cat("Medical", "Medical", "Medical Facility", "Equipment", "General Maintenance=Civil & General Maintenance");
        cat("Other", "Other / General", "Other");
    }

    private void cat(String parentName, String parentDept, String... subs) {
        Category parent = new Category();
        parent.setName(parentName);
        parent.setDepartment(depts.get(parentDept));
        parent = categoryRepository.save(parent);
        for (String s : subs) {
            String name = s;
            Department dept = depts.get(parentDept);
            int eq = s.lastIndexOf('=');
            if (eq > 0) { name = s.substring(0, eq); dept = depts.get(s.substring(eq + 1)); }
            Category sub = new Category();
            sub.setName(name);
            sub.setParent(parent);
            sub.setDepartment(dept);
            categoryRepository.save(sub);
        }
    }

    /**
     * The six confirmed campus locations. Floors are a generic sample structure (admins can edit them);
     * NO room numbers are seeded because real GL Bajaj room numbers are not known.
     */
    private void seedLocations() {
        String[] floors = {"Ground Floor", "1st Floor", "2nd Floor", "3rd Floor"};
        building("Block A", "Academic Block", floors, Map.of());
        building("Block B", "Academic Block", floors, Map.of());
        building("Cafeteria", "Cafeteria / Mess", new String[]{"Ground Floor"}, Map.of());
        building("Kalpana Chawla Hostel", "Hostel", floors, Map.of());
        building("APJ Abdul Kalam Hostel", "Hostel", floors, Map.of());
        building("Gym", "Sports Area", new String[]{}, Map.of());
    }

    private void building(String name, String type, String[] floors, Map<String, String[]> rooms) {
        Location b = new Location();
        b.setName(name); b.setType(type); b.setLevel(LocationLevel.BUILDING);
        b = locationRepository.save(b);
        for (String f : floors) {
            Location fl = new Location();
            fl.setName(f); fl.setType(type); fl.setLevel(LocationLevel.FLOOR); fl.setParent(b); fl.setFloor(f);
            fl = locationRepository.save(fl);
            for (String r : rooms.getOrDefault(f, new String[0])) {
                Location room = new Location();
                room.setName(r); room.setType(type); room.setLevel(LocationLevel.ROOM); room.setParent(fl); room.setFloor(f);
                locationRepository.save(room);
            }
        }
    }

    private void loadMaps() {
        loadDepartments();
        cats.clear(); locs.clear();
        for (Category c : categoryRepository.findAll()) {
            cats.put(c.getParent() == null ? c.getName() : c.getParent().getName() + "/" + c.getName(), c);
        }
        for (Location l : locationRepository.findAll()) locs.put(EntityMapper.locationPath(l).replace(" > ", "/"), l);
    }

    // ================================================================= users
    private User createAdmin() {
        User u = new User();
        u.setName("Campus Admin");
        u.setEmail(ADMIN_EMAIL);
        u.setPhone("9876500000");
        u.setPassword(encoder.encode(ADMIN_PASSWORD));
        u.setRole(Role.ADMIN);
        return userRepository.save(u);
    }

    private List<Student> seedStudents() {
        String[][] rows = {{"Riya Verma", "student1@campuscare.local", "DEMO-STU-001", "9876510001"},
                {"Aman Gupta", "student2@campuscare.local", "DEMO-STU-002", "9876510002"},
                {"Sneha Singh", "student3@campuscare.local", "DEMO-STU-003", "9876510003"}};
        List<Student> out = new ArrayList<>();
        for (String[] r : rows) {
            User u = new User();
            u.setName(r[0]); u.setEmail(r[1]); u.setPhone(r[3]);
            u.setPassword(encoder.encode(STUDENT_PASSWORD));
            u.setRole(Role.STUDENT);
            u = userRepository.save(u);
            Student s = new Student();
            s.setUser(u); s.setStudentId(r[2]); s.setCourse("B.Tech"); s.setBranch("Computer Science & Engineering");
            s.setYear(3); s.setSection("A");
            out.add(studentRepository.save(s));
        }
        return out;
    }

    /** key, name, email, employee id, department, designation, active? */
    private Map<String, Staff> seedStaff() {
        Object[][] rows = {
                {"it", "Rahul Mehta", "staff.it@campuscare.local", "DEMO-EMP-001", "IT & Wi-Fi Support", "Network Technician", true},
                {"elec", "Vikram Yadav", "staff.electrical@campuscare.local", "DEMO-EMP-002", "Electrical & Power", "Electrician", true},
                {"plumb", "Suresh Kumar", "staff.plumbing@campuscare.local", "DEMO-EMP-003", "Plumbing & Water", "Plumber", true},
                {"hk", "Anita Devi", "staff.housekeeping@campuscare.local", "DEMO-EMP-004", "Housekeeping & Sanitation", "Housekeeping Supervisor", true},
                {"ac", "Deepak Chauhan", "staff.ac@campuscare.local", "DEMO-EMP-005", "AC & Cooling", "AC Technician", true},
                {"hostel", "Manoj Tiwari", "staff.hostel@campuscare.local", "DEMO-EMP-006", "Hostel", "Hostel Caretaker", true},
                {"mess", "Kavita Rani", "staff.mess@campuscare.local", "DEMO-EMP-007", "Mess & Cafeteria", "Mess Supervisor", true},
                {"sec", "Ramesh Pal", "staff.security@campuscare.local", "DEMO-EMP-008", "Security", "Security Supervisor", true},
                {"furn", "Imran Ali", "staff.furniture@campuscare.local", "DEMO-EMP-009", "Civil & General Maintenance", "Carpenter", true},
                {"former", "Sanjay Dubey", "staff.former@campuscare.local", "DEMO-EMP-010", "Civil & General Maintenance", "Carpenter (left the institute)", false}};
        Map<String, Staff> out = new HashMap<>();
        long phone = 9876520001L;
        for (Object[] r : rows) {
            User u = new User();
            u.setName((String) r[1]); u.setEmail((String) r[2]); u.setPhone(String.valueOf(phone++));
            u.setPassword(encoder.encode(STAFF_PASSWORD));
            u.setRole(Role.STAFF);
            u.setStatus((Boolean) r[6] ? UserStatus.ACTIVE : UserStatus.INACTIVE);   // "former" demonstrates a deactivated staff member
            u = userRepository.save(u);
            Staff s = new Staff();
            s.setUser(u); s.setEmployeeId((String) r[3]); s.setDepartment(depts.get((String) r[4])); s.setDesignation((String) r[5]);
            out.put((String) r[0], staffRepository.save(s));
        }
        return out;
    }

    // ================================================================= demo issues
    private record DemoIssue(String title, String description, String category, String location, int student, Priority priority,
                             IssueStatus status, int hoursAgo, String staffKey, String note, Integer rating, String feedback) {}

    private List<DemoIssue> demoIssues() {
        String A2 = "Block A/2nd Floor";
        return List.of(
            new DemoIssue("Wi-Fi not working in the computer lab", "Internet is not working on all computers in the lab since morning. Students cannot submit online assignments.",
                    "IT & Internet/Wi-Fi / Internet", A2, 0, Priority.HIGH, IssueStatus.IN_PROGRESS, 30, "it", null, null, null),
            new DemoIssue("Tube-light flickering in a classroom in Block B", "One tube-light keeps flickering during lectures and gives everyone a headache.",
                    "Electrical/Light", "Block B/1st Floor", 2, Priority.LOW, IssueStatus.ASSIGNED, 20, "elec", null, null, null),
            new DemoIssue("Water leakage in washroom near Block A ground floor", "Water is leaking from the tap and the floor is always wet and slippery.",
                    "Plumbing & Water/Water Leakage", "Block A/Ground Floor", 1, Priority.MEDIUM, IssueStatus.REPORTED, 60, null, null, null, null),
            new DemoIssue("Sparks coming from switchboard in hostel room", "There are sparks and a burning smell from the switchboard near the bed. Very unsafe.",
                    "Electrical/Other Electrical", "APJ Abdul Kalam Hostel/1st Floor", 1, Priority.CRITICAL, IssueStatus.REPORTED, 10, null, null, null, null),
            new DemoIssue("No water supply on 2nd floor of the girls hostel", "There is no water in any bathroom on the second floor since last night.",
                    "Plumbing & Water/Water Supply", "Kalpana Chawla Hostel/2nd Floor", 0, Priority.HIGH, IssueStatus.ASSIGNED, 40, "plumb", null, null, null),
            new DemoIssue("Broken chair in a classroom in Block A", "The back support of a chair in the third row is broken.",
                    "Classroom & Lab/Desk/Chair", "Block A/1st Floor", 2, Priority.LOW, IssueStatus.RESOLVED, 100, "furn",
                    "Chair replaced with a new one from the store.", null, null),
            new DemoIssue("AC not cooling in a classroom in Block B", "The AC is running but the room stays warm during afternoon lectures.",
                    "Classroom & Lab/AC/Cooling", "Block B/1st Floor", 0, Priority.MEDIUM, IssueStatus.CLOSED, 140, "ac",
                    "Gas refilled and filters cleaned. Cooling is normal now.", 5, "Fixed quickly, thank you!"),
            new DemoIssue("Washroom not cleaned in Block B", "The washroom on the ground floor has not been cleaned for two days.",
                    "Cleaning & Sanitation/Washroom Cleaning", "Block B/Ground Floor", 2, Priority.MEDIUM, IssueStatus.CLOSED, 120, "hk",
                    "Deep cleaning done and cleaning schedule updated.", 4, "Clean now. Please keep the schedule."),
            new DemoIssue("Cafeteria table is wobbly", "One of the dining tables wobbles and spills food.",
                    "Cafeteria / Mess/Furniture", "Cafeteria/Ground Floor", 1, Priority.LOW, IssueStatus.CLOSED, 170, "furn",
                    "Table leg fixed and tightened.", 3, "Okay, but it took a few days."),
            new DemoIssue("Projector shows no display in a classroom in Block A", "The projector turns on but nothing appears on the screen.",
                    "IT & Internet/Projector/Smart Board", "Block A/2nd Floor", 1, Priority.MEDIUM, IssueStatus.REOPENED, 90, "it",
                    "HDMI cable replaced.", null, null),
            new DemoIssue("Treadmill belt is broken in the gym", "The belt of one treadmill is torn and unsafe to use.",
                    "Gym & Sports/Gym Equipment", "Gym", 2, Priority.MEDIUM, IssueStatus.REPORTED, 8, null, null, null, null),
            new DemoIssue("Request to change hostel room", "I would like to shift to another room on the same floor.",
                    "Hostel/Room Maintenance", "Kalpana Chawla Hostel/1st Floor", 0, Priority.LOW, IssueStatus.REJECTED, 96, null,
                    "This is a room-allotment request. Please contact the hostel warden's office.", null, null),
            new DemoIssue("RO water purifier not working in cafeteria", "The RO purifier gives no water and the indicator light is off.",
                    "Cafeteria / Mess/Water", "Cafeteria/Ground Floor", 2, Priority.HIGH, IssueStatus.ASSIGNED, 5, "plumb", null, null, null),
            new DemoIssue("Garbage not collected near hostel entrance", "Garbage bins near the hostel gate have overflowed and smell bad.",
                    "Cleaning & Sanitation/Garbage", "APJ Abdul Kalam Hostel/Ground Floor", 1, Priority.LOW, IssueStatus.IN_PROGRESS, 26, "hk", null, null, null),
            new DemoIssue("Window latch broken in gym area", "The window latch is broken and the window will not close properly.",
                    "Gym & Sports/Facility Maintenance", "Gym", 2, Priority.LOW, IssueStatus.ASSIGNED, 70, "former", null, null, null),
            new DemoIssue("Power cut in Block B first-floor classrooms", "There is no power in all classrooms on the first floor. Lectures are affected.",
                    "Electrical/Power Supply", "Block B/1st Floor", 0, Priority.HIGH, IssueStatus.IN_PROGRESS, 12, "elec", null, null, null),
            new DemoIssue("Washing machine not working in girls hostel", "The shared washing machine does not start.",
                    "Hostel/Common Area", "Kalpana Chawla Hostel/Ground Floor", 0, Priority.LOW, IssueStatus.CLOSED, 200, "hostel",
                    "Power connector replaced. Machine working.", 5, "Works perfectly now."),
            new DemoIssue("Room fan makes loud noise", "The ceiling fan in my room makes a loud grinding noise.",
                    "Electrical/Fan", "APJ Abdul Kalam Hostel/2nd Floor", 1, Priority.LOW, IssueStatus.REPORTED, 3, null, null, null, null),
            new DemoIssue("Door lock broken in the lab in Block B", "The lab door lock is jammed and the lab cannot be locked at night.",
                    "Classroom & Lab/Furniture", "Block B/2nd Floor", 2, Priority.MEDIUM, IssueStatus.CLOSED, 230, "former",
                    "Lock replaced.", 4, "Done, thanks."));
    }

    private static List<IssueStatus> pathFor(IssueStatus finalStatus) {
        return switch (finalStatus) {
            case REPORTED -> List.of(IssueStatus.REPORTED);
            case ASSIGNED -> List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED);
            case IN_PROGRESS -> List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS);
            case RESOLVED -> List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED);
            case CLOSED -> List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, IssueStatus.CLOSED);
            case REJECTED -> List.of(IssueStatus.REPORTED, IssueStatus.REJECTED);
            case REOPENED -> List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, IssueStatus.REOPENED);
        };
    }

    private void seedIssues(User admin, List<Student> students, Map<String, Staff> staffByKey) {
       LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        int number = 1001;
        for (DemoIssue d : demoIssues()) {
            Student student = students.get(d.student());
            Staff staff = d.staffKey() == null ? null : staffByKey.get(d.staffKey());
            List<IssueStatus> path = pathFor(d.status());
            LocalDateTime[] times = new LocalDateTime[path.size()];
            LocalDateTime t0 = now.minusHours(d.hoursAgo());
            for (int i = 0; i < times.length; i++) {
                times[i] = i == 0 ? t0 : t0.plus(Duration.ofMinutes((long) (d.hoursAgo() * 60 * 0.9 * i / (times.length - 1))));
            }

            Issue issue = new Issue();
            issue.setIssueNumber("CC-" + (number++));
            issue.setTitle(d.title());
            issue.setDescription(d.description());
            issue.setCategory(Objects.requireNonNull(cats.get(d.category()), "Unknown demo category " + d.category()));
            issue.setLocation(Objects.requireNonNull(locs.get(d.location()), "Unknown demo location " + d.location()));
            issue.setStudent(student);
            issue.setPriority(d.priority());
            issue.setStatus(d.status());
            issue.setContactNumber(student.getUser().getPhone());
            issue.setCreatedAt(t0);
            issue.setUpdatedAt(times[times.length - 1]);
            if (staff != null && path.contains(IssueStatus.ASSIGNED)) {
                issue.setAssignedStaff(staff);
                issue.setAssignedDepartment(staff.getDepartment());
            }
            for (int i = 0; i < path.size(); i++) {
                if (path.get(i) == IssueStatus.RESOLVED && d.status() != IssueStatus.REOPENED) {
                    issue.setResolvedAt(times[i]);
                    issue.setResolutionNote(d.note());
                }
                if (path.get(i) == IssueStatus.CLOSED) issue.setClosedAt(times[i]);
            }
            issue = issueRepository.save(issue);

            if (issue.getAssignedStaff() != null) {
                IssueAssignment a = new IssueAssignment();
                a.setIssue(issue); a.setStaff(staff); a.setAssignedBy(admin);
                a.setAssignedAt(times[path.indexOf(IssueStatus.ASSIGNED)]);
                assignmentRepository.save(a);
            }
            for (int i = 0; i < path.size(); i++) {
                IssueStatus s = path.get(i);
                User actor = switch (s) {
                    case REPORTED, CLOSED, REOPENED -> student.getUser();
                    case ASSIGNED, REJECTED -> admin;
                    default -> staff != null ? staff.getUser() : admin;
                };
                String comment = switch (s) {
                    case REPORTED -> "Issue reported";
                    case ASSIGNED -> "Assigned to " + staff.getUser().getName() + " (" + staff.getDepartment().getName() + ")";
                    case IN_PROGRESS -> "Work started";
                    case RESOLVED -> "Resolution note: " + d.note();
                    case CLOSED -> "Student confirmed the resolution";
                    case REJECTED -> "Rejected: " + d.note();
                    case REOPENED -> "Student reopened the issue: The problem is still there after the fix.";
                };
                IssueStatusHistory h = new IssueStatusHistory();
                h.setIssue(issue); h.setOldStatus(i == 0 ? null : path.get(i - 1)); h.setNewStatus(s);
                h.setChangedBy(actor); h.setComment(comment); h.setChangedAt(times[i]);
                historyRepository.save(h);

                String title = switch (s) {
                    case REPORTED -> "Issue created"; case ASSIGNED -> "Issue assigned"; case IN_PROGRESS -> "Work started";
                    case RESOLVED -> "Issue resolved"; case CLOSED -> "Issue closed"; case REJECTED -> "Issue rejected"; case REOPENED -> "Issue reopened";
                };
                notification(student.getUser(), issue, title, issue.getIssueNumber() + ": " + title.toLowerCase() + ".", times[i], now);
                if (s == IssueStatus.REPORTED) notification(admin, issue, "New issue reported", issue.getIssueNumber() + ": " + d.title(), times[i], now);
                if (s == IssueStatus.ASSIGNED && staff != null) notification(staff.getUser(), issue, "New assignment", issue.getIssueNumber() + ": " + d.title(), times[i], now);
                if (s == IssueStatus.REOPENED) notification(admin, issue, "Issue reopened", issue.getIssueNumber() + " was reopened by the student.", times[i], now);
            }
            if (d.rating() != null) {
                Feedback f = new Feedback();
                f.setIssue(issue); f.setStudent(student); f.setRating(d.rating()); f.setComment(d.feedback());
                f.setCreatedAt(times[times.length - 1].plusHours(1));
                feedbackRepository.save(f);
            }
        }
    }

    private void notification(User user, Issue issue, String title, String message, LocalDateTime at, LocalDateTime now) {
        Notification n = new Notification();
        n.setUser(user); n.setIssue(issue); n.setTitle(title); n.setMessage(message);
        n.setCreatedAt(at);
        n.setRead(at.isBefore(now.minusHours(24)));
        notificationRepository.save(n);
    }
}
