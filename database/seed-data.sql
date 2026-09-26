-- GL Bajaj CampusCare - reference data: departments, location types, categories, campus locations.
-- Departments and categories follow the project specification. Department 'description' holds the department GROUP.
-- Locations are the six confirmed campus locations. Floors are a generic sample structure; NO room numbers are seeded (real room numbers are unknown).
-- NOT included: user accounts (passwords need BCrypt, so the backend creates the admin and the optional DEMO users/issues on first start).
-- Not executed against MySQL in the authoring environment. Run schema.sql first. Skip this file if the backend already seeded the database.
USE gl_bajaj_campuscare;

INSERT INTO departments (name, description, status) VALUES
  ('Electrical & Power', 'Facility & Maintenance', 'ACTIVE'),
  ('Plumbing & Water', 'Facility & Maintenance', 'ACTIVE'),
  ('Civil & General Maintenance', 'Facility & Maintenance', 'ACTIVE'),
  ('Housekeeping & Sanitation', 'Facility & Maintenance', 'ACTIVE'),
  ('AC & Cooling', 'Facility & Maintenance', 'ACTIVE'),
  ('IT & Wi-Fi Support', 'Technology', 'ACTIVE'),
  ('Computer Labs', 'Technology', 'ACTIVE'),
  ('Classroom Facilities', 'Academic Facilities', 'ACTIVE'),
  ('Laboratory Facilities', 'Academic Facilities', 'ACTIVE'),
  ('Library', 'Academic Facilities', 'ACTIVE'),
  ('Language Lab', 'Academic Facilities', 'ACTIVE'),
  ('Hostel', 'Student Facilities', 'ACTIVE'),
  ('Mess & Cafeteria', 'Student Facilities', 'ACTIVE'),
  ('Medical', 'Student Facilities', 'ACTIVE'),
  ('Sports & Gym', 'Student Facilities', 'ACTIVE'),
  ('Transport', 'Campus Operations', 'ACTIVE'),
  ('Security', 'Campus Operations', 'ACTIVE'),
  ('Administration', 'Campus Operations', 'ACTIVE'),
  ('Other / General', 'Campus Operations', 'ACTIVE');

INSERT INTO location_types (name, status) VALUES
  ('Academic Block', 'ACTIVE'),
  ('Hostel', 'ACTIVE'),
  ('Cafeteria / Mess', 'ACTIVE'),
  ('Library', 'ACTIVE'),
  ('Sports Area', 'ACTIVE'),
  ('Auditorium / Seminar Hall', 'ACTIVE'),
  ('Laboratory Block', 'ACTIVE'),
  ('Administrative Block', 'ACTIVE'),
  ('Parking Area', 'ACTIVE'),
  ('Ground / Garden', 'ACTIVE'),
  ('Medical Room', 'ACTIVE'),
  ('Other', 'ACTIVE');

INSERT INTO categories (name, department_id, parent_id, status) VALUES ('IT & Internet', (SELECT id FROM departments WHERE name='IT & Wi-Fi Support'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Wi-Fi / Internet', (SELECT id FROM departments WHERE name='IT & Wi-Fi Support'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Computer/System', (SELECT id FROM departments WHERE name='IT & Wi-Fi Support'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Network', (SELECT id FROM departments WHERE name='IT & Wi-Fi Support'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Projector/Smart Board', (SELECT id FROM departments WHERE name='Classroom Facilities'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Electrical', (SELECT id FROM departments WHERE name='Electrical & Power'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Light', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Fan', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Switch/Socket', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Power Supply', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other Electrical', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Plumbing & Water', (SELECT id FROM departments WHERE name='Plumbing & Water'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Water Leakage', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Tap', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Washroom', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Drainage', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Water Supply', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Cleaning & Sanitation', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Room/Classroom Cleaning', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Washroom Cleaning', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Garbage', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Pest/General Hygiene', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Classroom & Lab', (SELECT id FROM departments WHERE name='Classroom Facilities'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Furniture', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Desk/Chair', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Board', (SELECT id FROM departments WHERE name='Classroom Facilities'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Lab Equipment', (SELECT id FROM departments WHERE name='Laboratory Facilities'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('AC/Cooling', (SELECT id FROM departments WHERE name='AC & Cooling'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other Classroom/Lab', (SELECT id FROM departments WHERE name='Classroom Facilities'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Hostel', (SELECT id FROM departments WHERE name='Hostel'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Room Maintenance', (SELECT id FROM departments WHERE name='Hostel'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Furniture', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Water', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Electrical', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Cleaning', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Common Area', (SELECT id FROM departments WHERE name='Hostel'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Library', (SELECT id FROM departments WHERE name='Library'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Furniture', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Computer/Internet', (SELECT id FROM departments WHERE name='IT & Wi-Fi Support'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Lighting', (SELECT id FROM departments WHERE name='Electrical & Power'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('AC', (SELECT id FROM departments WHERE name='AC & Cooling'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other', (SELECT id FROM departments WHERE name='Library'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Cafeteria / Mess', (SELECT id FROM departments WHERE name='Mess & Cafeteria'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Food-related facility issue', (SELECT id FROM departments WHERE name='Mess & Cafeteria'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Water', (SELECT id FROM departments WHERE name='Plumbing & Water'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Cleaning', (SELECT id FROM departments WHERE name='Housekeeping & Sanitation'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Equipment', (SELECT id FROM departments WHERE name='Mess & Cafeteria'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Furniture', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Gym & Sports', (SELECT id FROM departments WHERE name='Sports & Gym'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Gym Equipment', (SELECT id FROM departments WHERE name='Sports & Gym'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Sports Equipment', (SELECT id FROM departments WHERE name='Sports & Gym'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Facility Maintenance', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Security', (SELECT id FROM departments WHERE name='Security'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('CCTV', (SELECT id FROM departments WHERE name='Security'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Access/Security', (SELECT id FROM departments WHERE name='Security'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Gate/Barrier', (SELECT id FROM departments WHERE name='Security'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other Security', (SELECT id FROM departments WHERE name='Security'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Transport', (SELECT id FROM departments WHERE name='Transport'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Bus', (SELECT id FROM departments WHERE name='Transport'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Parking', (SELECT id FROM departments WHERE name='Transport'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Transport Facility', (SELECT id FROM departments WHERE name='Transport'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Medical', (SELECT id FROM departments WHERE name='Medical'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Medical Facility', (SELECT id FROM departments WHERE name='Medical'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Equipment', (SELECT id FROM departments WHERE name='Medical'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('General Maintenance', (SELECT id FROM departments WHERE name='Civil & General Maintenance'), @parent, 'ACTIVE');
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other', (SELECT id FROM departments WHERE name='Other / General'), NULL, 'ACTIVE');
SET @parent := LAST_INSERT_ID();
INSERT INTO categories (name, department_id, parent_id, status) VALUES ('Other', (SELECT id FROM departments WHERE name='Other / General'), @parent, 'ACTIVE');

INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Block A', 'Academic Block', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Ground Floor', 'Academic Block', 'FLOOR', @building, 'Ground Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('1st Floor', 'Academic Block', 'FLOOR', @building, '1st Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('2nd Floor', 'Academic Block', 'FLOOR', @building, '2nd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('3rd Floor', 'Academic Block', 'FLOOR', @building, '3rd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Block B', 'Academic Block', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Ground Floor', 'Academic Block', 'FLOOR', @building, 'Ground Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('1st Floor', 'Academic Block', 'FLOOR', @building, '1st Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('2nd Floor', 'Academic Block', 'FLOOR', @building, '2nd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('3rd Floor', 'Academic Block', 'FLOOR', @building, '3rd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Cafeteria', 'Cafeteria / Mess', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Ground Floor', 'Cafeteria / Mess', 'FLOOR', @building, 'Ground Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Kalpana Chawla Hostel', 'Hostel', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Ground Floor', 'Hostel', 'FLOOR', @building, 'Ground Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('1st Floor', 'Hostel', 'FLOOR', @building, '1st Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('2nd Floor', 'Hostel', 'FLOOR', @building, '2nd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('3rd Floor', 'Hostel', 'FLOOR', @building, '3rd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('APJ Abdul Kalam Hostel', 'Hostel', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Ground Floor', 'Hostel', 'FLOOR', @building, 'Ground Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('1st Floor', 'Hostel', 'FLOOR', @building, '1st Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('2nd Floor', 'Hostel', 'FLOOR', @building, '2nd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('3rd Floor', 'Hostel', 'FLOOR', @building, '3rd Floor', 'ACTIVE');
INSERT INTO locations (name, type, location_level, parent_id, floor_label, status) VALUES ('Gym', 'Sports Area', 'BUILDING', NULL, NULL, 'ACTIVE');
SET @building := LAST_INSERT_ID();
