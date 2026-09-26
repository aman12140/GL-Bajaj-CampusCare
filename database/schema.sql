-- GL Bajaj CampusCare - reference MySQL 8 schema.
--
-- The application creates/updates these tables AUTOMATICALLY through Hibernate (spring.jpa.hibernate.ddl-auto=update),
-- so you do NOT need to run this file for a normal start.
-- Use it as documentation, or to create the schema manually (then start the backend with DDL_AUTO=none, not validate).
-- NOTE: table and column names were cross-checked against the JPA entities (static comparison only). It has NOT been executed against a MySQL server.

CREATE DATABASE IF NOT EXISTS gl_bajaj_campuscare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gl_bajaj_campuscare;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,
  password VARCHAR(255) NOT NULL,                 -- BCrypt hash only
  phone VARCHAR(20),
  role VARCHAR(20) NOT NULL,                      -- STUDENT | STAFF | ADMIN
  status VARCHAR(20) NOT NULL,                    -- ACTIVE | INACTIVE
  password_change_required BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS departments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(300),
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_departments_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  student_id VARCHAR(30) NOT NULL,
  course VARCHAR(60) NOT NULL,
  branch VARCHAR(60) NOT NULL,
  study_year INT NOT NULL,                        -- "year" is a reserved word in some databases
  section VARCHAR(10) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_students_user (user_id),
  UNIQUE KEY uk_students_student_id (student_id),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS staff (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  designation VARCHAR(100) NOT NULL,
  employee_id VARCHAR(30) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_staff_user (user_id),
  UNIQUE KEY uk_staff_employee_id (employee_id),
  CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_staff_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS location_types (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_location_types_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS locations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(80) NOT NULL,                      -- location type name, inherited from the building
  location_level VARCHAR(20) NOT NULL,            -- BUILDING | FLOOR | ROOM
  parent_id BIGINT,
  floor_label VARCHAR(40),
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_locations_parent (parent_id),
  CONSTRAINT fk_locations_parent FOREIGN KEY (parent_id) REFERENCES locations (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(300),
  department_id BIGINT,                           -- suggested department
  parent_id BIGINT,                               -- NULL = top-level category
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_categories_department FOREIGN KEY (department_id) REFERENCES departments (id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS issues (
  id BIGINT NOT NULL AUTO_INCREMENT,
  issue_number VARCHAR(40) NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  category_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  room_number VARCHAR(40),                        -- free-text room number typed by the student (optional)
  student_id BIGINT NOT NULL,
  priority VARCHAR(20) NOT NULL,                  -- LOW | MEDIUM | HIGH | CRITICAL
  status VARCHAR(20) NOT NULL,                    -- REPORTED | ASSIGNED | IN_PROGRESS | RESOLVED | CLOSED | REJECTED | REOPENED
  image_url VARCHAR(300),
  contact_number VARCHAR(20),
  assigned_staff_id BIGINT,
  assigned_department_id BIGINT,
  resolution_note VARCHAR(1000),
  resolution_image_url VARCHAR(300),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  resolved_at DATETIME(6),
  closed_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_issues_number (issue_number),
  KEY idx_issues_status (status),
  KEY idx_issues_priority (priority),
  KEY idx_issues_created (created_at),
  KEY idx_issues_student (student_id),
  KEY idx_issues_staff (assigned_staff_id),
  CONSTRAINT fk_issues_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_issues_location FOREIGN KEY (location_id) REFERENCES locations (id),
  CONSTRAINT fk_issues_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_issues_staff FOREIGN KEY (assigned_staff_id) REFERENCES staff (id),
  CONSTRAINT fk_issues_department FOREIGN KEY (assigned_department_id) REFERENCES departments (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS issue_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  assigned_at DATETIME(6) NOT NULL,
  unassigned_at DATETIME(6),                      -- NULL while the assignment is current
  PRIMARY KEY (id),
  KEY idx_assign_issue (issue_id),
  CONSTRAINT fk_assign_issue FOREIGN KEY (issue_id) REFERENCES issues (id),
  CONSTRAINT fk_assign_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
  CONSTRAINT fk_assign_by FOREIGN KEY (assigned_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS issue_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  old_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL,
  changed_by BIGINT NOT NULL,
  comment VARCHAR(1000),
  changed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_history_issue (issue_id),
  CONSTRAINT fk_history_issue FOREIGN KEY (issue_id) REFERENCES issues (id),
  CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  issue_id BIGINT,
  title VARCHAR(150) NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_notif_user (user_id, is_read),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_notif_issue FOREIGN KEY (issue_id) REFERENCES issues (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment VARCHAR(1000),
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_feedback_issue (issue_id),        -- one feedback per issue
  CONSTRAINT fk_feedback_issue FOREIGN KEY (issue_id) REFERENCES issues (id),
  CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,                -- SHA-256 of the token, never the token itself
  expires_at DATETIME(6) NOT NULL,
  used BIT(1) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reset_token_hash (token_hash),
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
