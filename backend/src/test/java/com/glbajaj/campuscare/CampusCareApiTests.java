package com.glbajaj.campuscare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glbajaj.campuscare.repository.CategoryRepository;
import com.glbajaj.campuscare.repository.DepartmentRepository;
import com.glbajaj.campuscare.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests using the in-memory H2 test profile and the seeded demo data.
 * Each test creates its own issues/users, so the tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampusCareApiTests {
    private static final String ADMIN = "admin@campuscare.local";
    private static final String STUDENT1 = "student1@campuscare.local";
    private static final String STUDENT2 = "student2@campuscare.local";
    private static final String STAFF_IT = "staff.it@campuscare.local";
    private static final String STAFF_ELEC = "staff.electrical@campuscare.local";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categoryRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired DepartmentRepository departmentRepository;

    // ------------------------------------------------------------------ helpers
    private String bearer(String token) { return "Bearer " + token; }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private String loginAdmin() throws Exception { return login(ADMIN, "Admin@123"); }
    private String loginStudent(String email) throws Exception { return login(email, "Student@123"); }
    private String loginStaff(String email) throws Exception { return login(email, "Staff@123"); }

    private long staffId(String adminToken, String email) throws Exception {
        String body = mvc.perform(get("/api/admin/staff").param("q", email).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get(0).get("id").asLong();
    }

    private long createIssue(String studentToken, String title, String priority) throws Exception {
        long categoryId = categoryRepository.findAll().stream().filter(c -> c.getName().equals("Wi-Fi / Internet")).findFirst().orElseThrow().getId();
        long locationId = locationRepository.findAll().stream().filter(l -> l.getName().equals("Block A")).findFirst().orElseThrow().getId();
        String body = mvc.perform(post("/api/issues").header("Authorization", bearer(studentToken))
                        .param("title", title).param("categoryId", String.valueOf(categoryId)).param("locationId", String.valueOf(locationId))
                        .param("priority", priority).param("description", "The internet has stopped working in this area."))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private ResultActions assign(String adminToken, long issueId, long staffId) throws Exception {
        return mvc.perform(put("/api/issues/" + issueId + "/assign").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("staffId", staffId))));
    }

    private ResultActions setStatus(String token, long issueId, String status, String comment) throws Exception {
        var req = put("/api/issues/" + issueId + "/status").header("Authorization", bearer(token)).param("status", status);
        if (comment != null) req = req.param("comment", comment);
        return mvc.perform(req);
    }

    private JsonNode getIssue(String token, long id) throws Exception {
        String body = mvc.perform(get("/api/issues/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    /** Creates a fresh STAFF account through the admin API and returns its email. */
    private String createStaffViaApi(String adminToken) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "tmp" + suffix + "@campuscare.local";
        long deptId = departmentRepository.findByNameIgnoreCase("IT & Wi-Fi Support").orElseThrow().getId();
        mvc.perform(post("/api/admin/staff").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Temp Staff", "employeeId", "T" + suffix, "email", email,
                                "phone", "9876543210", "departmentId", deptId, "designation", "Technician", "temporaryPassword", "Temp@1234"))))
                .andExpect(status().isCreated());
        return email;
    }

    // ------------------------------------------------------------------ registration & login
    @Test
    void studentCanRegisterButDuplicateEmailIsRejected() throws Exception {
        String email = "new" + System.nanoTime() + "@campuscare.local";
        String body = json.writeValueAsString(Map.of("name", "New Student", "studentId", "NS" + System.nanoTime(), "email", email,
                "phone", "9876543211", "course", "B.Tech", "branch", "CSE", "year", 2, "section", "B",
                "password", "Passw0rd1", "confirmPassword", "Passw0rd1"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        String second = body.replaceAll("\"studentId\":\"[^\"]*\"", "\"studentId\":\"OTHER" + System.nanoTime() + "\"");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(second)).andExpect(status().isConflict());
        login(email, "Passw0rd1");   // the new student can log in
    }

    @Test
    void weakPasswordIsRejectedOnRegistration() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "Weak", "studentId", "W" + System.nanoTime(), "email", "weak" + System.nanoTime() + "@x.com",
                "phone", "9876543212", "course", "B.Tech", "branch", "CSE", "year", 1, "section", "A",
                "password", "short", "confirmPassword", "short"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginWorksAndWrongPasswordIsUnauthorized() throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", STUDENT1, "password", "Student@123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        assertThat(res).doesNotContain("password");                   // the hash must never be returned
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", STUDENT1, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ create & retrieve
    @Test
    void studentCreatesAndRetrievesIssueWithTimeline() throws Exception {
        String token = loginStudent(STUDENT1);
        long id = createIssue(token, "Wi-Fi is down near Block A", "HIGH");
        JsonNode issue = getIssue(token, id);
        assertThat(issue.get("issueNumber").asText()).startsWith("CC-");
        assertThat(issue.get("status").asText()).isEqualTo("REPORTED");
        assertThat(issue.get("timeline").get(0).get("newStatus").asText()).isEqualTo("REPORTED");
    }

    @Test
    void createIssueValidatesRequiredFields() throws Exception {
        mvc.perform(post("/api/issues").header("Authorization", bearer(loginStudent(STUDENT1))).param("title", "x"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ ownership & authorization
    @Test
    void studentCannotOpenAnotherStudentsIssue() throws Exception {
        long id = createIssue(loginStudent(STUDENT1), "Private issue of student one", "LOW");
        mvc.perform(get("/api/issues/" + id).header("Authorization", bearer(loginStudent(STUDENT2)))).andExpect(status().isForbidden());
    }

    @Test
    void studentAndStaffCannotUseAdminApis() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(loginStudent(STUDENT1)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/staff").header("Authorization", bearer(loginStaff(STAFF_IT)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/issues").header("Authorization", bearer(loginStudent(STUDENT1)))).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAssignIssues() throws Exception {
        String student = loginStudent(STUDENT1);
        long id = createIssue(student, "Student tries to assign", "LOW");
        assign(student, id, 1L).andExpect(status().isForbidden());
    }

    @Test
    void staffCannotChangeAnIssueAssignedToSomeoneElse() throws Exception {
        String admin = loginAdmin();
        long id = createIssue(loginStudent(STUDENT1), "Assigned to IT staff only", "MEDIUM");
        assign(admin, id, staffId(admin, STAFF_IT)).andExpect(status().isOk());
        setStatus(loginStaff(STAFF_ELEC), id, "IN_PROGRESS", null).andExpect(status().isForbidden());   // not assigned to this staff
        mvc.perform(get("/api/issues/" + id).header("Authorization", bearer(loginStaff(STAFF_ELEC)))).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ assignment & workflow
    @Test
    void fullWorkflowEndsWithStudentConfirmationAndSingleFeedback() throws Exception {
        String admin = loginAdmin(), student = loginStudent(STUDENT1), staff = loginStaff(STAFF_IT);
        long id = createIssue(student, "Full workflow issue", "HIGH");

        assign(admin, id, staffId(admin, STAFF_IT)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED"));
        setStatus(staff, id, "IN_PROGRESS", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        setStatus(staff, id, "RESOLVED", "Replaced the faulty router").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        setStatus(staff, id, "CLOSED", "trying to close").andExpect(status().isBadRequest());           // staff can never close

        mvc.perform(put("/api/issues/" + id + "/confirm").header("Authorization", bearer(student)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));

        String fb = json.writeValueAsString(Map.of("rating", 5, "comment", "Great"));
        mvc.perform(post("/api/issues/" + id + "/feedback").header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON).content(fb))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/issues/" + id + "/feedback").header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON).content(fb))
                .andExpect(status().isConflict());                                                       // only one feedback per issue
    }

    @Test
    void resolvingRequiresANote() throws Exception {
        String admin = loginAdmin(), staff = loginStaff(STAFF_IT);
        long id = createIssue(loginStudent(STUDENT1), "Needs resolution note", "LOW");
        assign(admin, id, staffId(admin, STAFF_IT)).andExpect(status().isOk());
        setStatus(staff, id, "IN_PROGRESS", null).andExpect(status().isOk());
        setStatus(staff, id, "RESOLVED", null).andExpect(status().isBadRequest());
    }

    @Test
    void illegalStatusJumpIsRejected() throws Exception {
        long id = createIssue(loginStudent(STUDENT1), "Cannot jump to resolved", "LOW");
        setStatus(loginAdmin(), id, "RESOLVED", "no work done yet").andExpect(status().isBadRequest());   // REPORTED -> RESOLVED is illegal
    }

    @Test
    void studentCanReopenWithReasonAndAdminReassigns() throws Exception {
        String admin = loginAdmin(), student = loginStudent(STUDENT1), staff = loginStaff(STAFF_IT);
        long id = createIssue(student, "Will be reopened", "MEDIUM");
        assign(admin, id, staffId(admin, STAFF_IT)).andExpect(status().isOk());
        setStatus(staff, id, "IN_PROGRESS", null).andExpect(status().isOk());
        setStatus(staff, id, "RESOLVED", "Tried a quick fix").andExpect(status().isOk());

        mvc.perform(put("/api/issues/" + id + "/reopen").header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("reason", "")))).andExpect(status().isBadRequest());   // reason is mandatory
        mvc.perform(put("/api/issues/" + id + "/reopen").header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("reason", "Still not working after the fix"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REOPENED"));

        assign(admin, id, staffId(admin, STAFF_ELEC)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED"));
        assertThat(getIssue(admin, id).get("assignedStaff").get("name").asText()).isNotBlank();
    }

    // ------------------------------------------------------------------ staff management
    @Test
    void newStaffMustChangeTemporaryPasswordBeforeUsingTheApp() throws Exception {
        String admin = loginAdmin();
        String email = createStaffViaApi(admin);
        String token = login(email, "Temp@1234");
        mvc.perform(get("/api/staff/dashboard").header("Authorization", bearer(token))).andExpect(status().isForbidden());   // blocked until changed
        mvc.perform(post("/api/auth/change-password").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("currentPassword", "Temp@1234", "newPassword", "Better@2024")))).andExpect(status().isOk());
        String fresh = login(email, "Better@2024");
        mvc.perform(get("/api/staff/dashboard").header("Authorization", bearer(fresh))).andExpect(status().isOk());
    }

    @Test
    void deactivatedStaffCannotLoginAndOldTokenStopsWorking() throws Exception {
        String admin = loginAdmin();
        String email = createStaffViaApi(admin);
        String oldToken = login(email, "Temp@1234");
        long id = staffId(admin, email);

        mvc.perform(put("/api/admin/staff/" + id + "/deactivate").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "Temp@1234"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(oldToken))).andExpect(status().isUnauthorized());

        long issueId = createIssue(loginStudent(STUDENT1), "Cannot assign to inactive staff", "LOW");
        assign(admin, issueId, id).andExpect(status().isBadRequest());                                   // inactive staff cannot get new work
    }

    @Test
    void duplicateStaffEmailIsRejected() throws Exception {
        String admin = loginAdmin();
        long deptId = departmentRepository.findByNameIgnoreCase("IT & Wi-Fi Support").orElseThrow().getId();
        mvc.perform(post("/api/admin/staff").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Dup", "employeeId", "DUP" + System.nanoTime(), "email", STAFF_IT,
                                "phone", "9876543210", "departmentId", deptId, "designation", "Tech", "temporaryPassword", "Temp@1234"))))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ priority, analytics, export
    @Test
    void prioritySuggestionIsRuleBased() throws Exception {
        String token = loginStudent(STUDENT1);
        mvc.perform(post("/api/issues/suggest-priority").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Internet not working", "description", "Internet is not working on all computers in the lab"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("HIGH"));
        mvc.perform(post("/api/issues/suggest-priority").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Sparks from the switchboard", "description", "There is a burning smell"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("CRITICAL"));
        mvc.perform(post("/api/issues/suggest-priority").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Firewall settings question", "description", "Please review the firewall configuration"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("LOW"));           // "firewall" must not count as "fire"
    }

    @Test
    void adminAnalyticsAndExportsWork() throws Exception {
        String admin = loginAdmin();
        mvc.perform(get("/api/admin/analytics").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cards.total").isNumber()).andExpect(jsonPath("$.byStatus").isArray());
        byte[] csv = mvc.perform(get("/api/admin/export/csv").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(csv, java.nio.charset.StandardCharsets.UTF_8)).contains("Issue No,Title,Category");
        byte[] pdf = mvc.perform(get("/api/admin/export/pdf").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void studentListShowsOnlyOwnIssues() throws Exception {
        String s1 = loginStudent(STUDENT1);
        createIssue(s1, "Visible only to student one", "LOW");
        String body = mvc.perform(get("/api/student/issues").param("size", "100").header("Authorization", bearer(loginStudent(STUDENT2))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Visible only to student one");
    }

    @Test
    void studentCanEnterRoomNumberAndItShowsInLocationAndSearch() throws Exception {
        String student = loginStudent(STUDENT1);
        long categoryId = categoryRepository.findAll().stream().filter(c -> c.getName().equals("Fan")).findFirst().orElseThrow().getId();
        long locationId = locationRepository.findAll().stream().filter(l -> l.getName().equals("Block A")).findFirst().orElseThrow().getId();
        var req = post("/api/issues").header("Authorization", bearer(student)).param("title", "Fan not working in room")
                .param("categoryId", String.valueOf(categoryId)).param("locationId", String.valueOf(locationId))
                .param("priority", "LOW").param("description", "The ceiling fan does not rotate at all.");
        String body = mvc.perform(req.param("roomNumber", "204-B")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("204-B")).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("locationPath").asText()).endsWith("Room 204-B");
        String list = mvc.perform(get("/api/student/issues").param("q", "204-b").header("Authorization", bearer(student)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("Room 204-B");                                           // searchable by room number
        mvc.perform(post("/api/issues").header("Authorization", bearer(student)).param("title", "Bad room value")
                .param("categoryId", String.valueOf(categoryId)).param("locationId", String.valueOf(locationId)).param("priority", "LOW")
                .param("description", "Room value contains forbidden characters.").param("roomNumber", "<script>"))
                .andExpect(status().isBadRequest());
    }
}
