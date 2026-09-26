# Database design

MySQL 8, 13 tables. Hibernate creates them from the JPA entities (`DDL_AUTO=update`); `database/schema.sql` is the matching
reference DDL. Enums are stored as text (`@Enumerated(STRING)`). It was compared with the entity classes by hand and has
never been executed on MySQL.

| Table | Purpose | Key columns / relations |
|---|---|---|
| users | Every account | `email` unique, BCrypt `password`, `role` (STUDENT/STAFF/ADMIN), `status`, `password_change_required` |
| students | Student profile | `user_id` (1:1 users), `student_id` unique, `course, branch, study_year, section` |
| departments | Departments | `name` unique, `description` (holds the department group), `status` |
| staff | Staff profile | `user_id` (1:1 users), `department_id`, `designation`, `employee_id` unique |
| location_types | Building types | `name` unique, `status` |
| locations | Building, Floor, Room tree | `parent_id` self-reference, `location_level`, `type`, `floor_label`, `status` |
| categories | Category / sub-category | `parent_id` self-reference, `department_id` (suggested department), `status` |
| issues | The reported issues | `issue_number` unique; free-text `room_number` (optional, typed by the student); FKs to category, location, student, assigned staff, assigned department; `priority`, `status`, timestamps |
| issue_assignments | Assignment history | issue, staff, `assigned_by` (users), `assigned_at`, `unassigned_at` (null = current) |
| issue_status_history | Timeline | issue, `old_status`, `new_status`, `changed_by`, `comment`, `changed_at` |
| notifications | In-app notifications | `user_id`, `issue_id`, `is_read`, `created_at` |
| feedback | Student rating | `issue_id` unique (one per issue), `student_id`, `rating`, `comment` |
| password_reset_tokens | Reset tokens | SHA-256 `token_hash` unique, `expires_at`, `used` |

Design choices: staff/departments/categories/locations are deactivated (status column), never deleted, so history stays
valid; the resolution note and photo live on the issue row; the full timeline lives in `issue_status_history`.
Enum values: Priority LOW/MEDIUM/HIGH/CRITICAL; IssueStatus REPORTED/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/REJECTED/REOPENED;
RecordStatus and UserStatus ACTIVE/INACTIVE; LocationLevel BUILDING/FLOOR/ROOM.

Demo data (students, staff, issues) is created by `DataSeeder.java` because passwords must be BCrypt-hashed; `seed-data.sql`
contains reference data only (departments, categories, the six campus locations with generic floors, no room numbers).
