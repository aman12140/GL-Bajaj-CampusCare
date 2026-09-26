# GL Bajaj CampusCare - Smart Campus Issue Management System

A full-stack college project: students report campus problems, the admin assigns them to the right department staff,
staff fix them, and the student confirms the fix. Every step is recorded.

> This is a student project. It is **not** an official GL Bajaj Group of Institutions system. All demo people, IDs and
> issues are fictional sample data.

## 1. What is in the project

| Part | Technology |
|---|---|
| Frontend | React 18, Vite 5, React Router 6, Axios, Recharts (plain CSS, no UI kit) |
| Backend | Java 21, Spring Boot 3.3.5, Spring Security (JWT + BCrypt), Spring Data JPA, OpenPDF |
| Database | MySQL 8 (H2 in-memory is used only by the automated tests) |

```
GL-Bajaj-CampusCare/
  backend/    Spring Boot API (src/main/java, src/test/java, pom.xml)
  frontend/   React app (src/, public/assets/gl-bajaj-logo.webp)
  database/   schema.sql (reference DDL) and seed-data.sql (reference data)
  docs/       API-Documentation.md, Database-Design.md
  screenshots/  (empty - see the note inside)
```

## 2. Features

- Roles: **Student**, **Staff**, **Admin**. Only students self-register; staff accounts are created by the admin (with a temporary password that must be changed at first login).
- Issue lifecycle: Reported, Assigned, In Progress, Resolved, Closed, plus Rejected and Reopened. Illegal jumps are rejected by one state machine (`IssueWorkflow`).
- Only the student can close an issue (by confirming the fix); a student can reopen it with a reason. One feedback (1-5 stars) per closed issue.
- Structured categories/sub-categories with a suggested department; structured locations (Building, Floor, optional Room) **plus a free-text Room No. field** on every issue (searchable, shown in lists, detail, CSV and PDF). Admins can also add fixed rooms from Admin > Locations ("Add room").
- Rule-based priority suggestion (keywords, shows its reasons, no AI). Demo SLA targets: Low 72 h, Medium 48 h, High 24 h, Critical 6 h (configurable demo values, not official policy).
- Photo upload (JPG/PNG, max 5 MB, content is checked, random file names).
- Notifications (polled every 30 s), filters and pagination on every list.
- Admin: staff management (deactivate instead of delete), departments, categories, locations, analytics with charts, CSV and PDF export (PDF header uses the GL Bajaj logo).
- Deactivated staff cannot log in and their existing tokens stop working immediately (the user is re-checked on every request).

## 3. Campus data used

Locations (exactly these six): **Block A, Block B, Cafeteria, Kalpana Chawla Hostel, APJ Abdul Kalam Hostel, Gym**.
Floors ("Ground Floor" ... "3rd Floor") are a *generic sample structure*, and **no room numbers are seeded** because real
GL Bajaj room numbers are not known. Students type their own Room No. when reporting; admins may also add real rooms in Admin > Locations ("Add room" on a building or floor).

Departments (19) and the category structure follow the project specification; see `DataSeeder.java` /
`database/seed-data.sql`. The **mapping of each category to a suggested department** is a reasonable default chosen while
building the project, and the admin can change it in the UI.

## 4. Logo

The supplied logo is used unchanged: `frontend/public/assets/gl-bajaj-logo.webp`. It is shown at a fixed height with
automatic width (never stretched). A lossless PNG copy of the same pixels is stored at
`backend/src/main/resources/branding/gl-bajaj-logo.png` only because the PDF library cannot read WebP.
The logo file has an opaque white background, so headers and sidebars are white. The path is defined in one place:
`frontend/src/config/branding.js`. To use an SVG later, put it in `frontend/public/assets/` and change `LOGO_URL`
(for the PDF, also replace the PNG in the backend `branding` folder).

## 5. What was verified, and what was NOT

Please read this before relying on the project.

**Verified (static checks only, in an offline sandbox):**
- All backend Java sources were compiled with `javac` directly. There are no syntax errors. Because Maven dependencies were unavailable, framework/Lombok symbols could not be resolved; a temporary copy with hand-generated getters/setters confirmed there are no unresolved method calls between the project's own classes.
- One tricky generic sort expression was compiled and run separately with plain JDK types.
- All 50 frontend `.js/.jsx` files were parsed with the TypeScript compiler (no syntax errors); every relative import was checked to exist and every named import to be exported.
- Frontend API calls were compared by hand with the controller paths, HTTP methods, request records and response records; JPA entity column names were compared with `database/schema.sql`.
- `package.json` is valid JSON.

**NOT verified:**
- `mvn test` / `mvn spring-boot:run` were **never run** (no Maven, and Maven Central was not reachable). The tests in `backend/src/test` were written and syntax-checked but **have never been executed**.
- `npm install` / `npm run build` were **never run** (npm registry not reachable), so the frontend has never been rendered in a browser; the layout, mobile behaviour and logo display are unchecked.
- **MySQL was never connected to.** `schema.sql` and `seed-data.sql` have not been executed against a database.
- File upload, PDF/CSV export, JWT flow and the scheduled polling have never been run end to end.

Expect to fix small compile/runtime issues on the first run. Dependency versions in `pom.xml` and `package.json` were chosen
from memory and may need adjusting.

## 6. Setup (local)

Prerequisites: JDK 21, Maven 3.9+, Node.js 18+ (20 recommended), MySQL 8.

1. **Database.** Start MySQL. The backend creates the database `gl_bajaj_campuscare` and its tables automatically
   (`DDL_AUTO=update`). Optional: run `database/schema.sql` yourself and start the backend with `DDL_AUTO=none`.
2. **Backend.**
   ```bash
   cd backend
   export DB_USERNAME=root DB_PASSWORD=your_password
   export JWT_SECRET="a-long-random-string-of-at-least-32-characters"
   mvn spring-boot:run          # http://localhost:8080
   mvn test                     # runs the tests on in-memory H2 (needs no MySQL)
   ```
   Settings (all optional environment variables): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `DDL_AUTO`,
   `UPLOAD_DIR`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, `SEED_DEMO_DATA`, `EXPOSE_RESET_LINK` (see `.env.example`).
3. **Frontend.**
   ```bash
   cd frontend
   npm install
   npm run dev                  # http://localhost:5173 (proxies /api and /uploads to :8080)
   npm run build                # production build in dist/
   ```
   Set `VITE_API_BASE_URL` only if the built site is served from a different host than the API.

## 7. Demo accounts (only when `SEED_DEMO_DATA=true`, the default)

| Role | Email | Password |
|---|---|---|
| Admin | admin@campuscare.local | Admin@123 |
| Student | student1@campuscare.local (also student2, student3) | Student@123 |
| Staff | staff.it@campuscare.local (also .electrical, .plumbing, .housekeeping, .ac, .hostel, .mess, .security, .furniture) | Staff@123 |
| Inactive staff (demo) | staff.former@campuscare.local | Staff@123 (cannot log in) |

On first start the seeder also creates 19 demo issues in every status, with timelines, notifications and feedback.
The admin account is created even when demo data is off. **Change or remove these accounts and set `SEED_DEMO_DATA=false`
before any real use.** Set `VITE_SHOW_DEMO_LOGINS=false` to hide the demo box on the login page.

## 8. Security notes

Passwords are hashed with BCrypt. JWTs are signed with `JWT_SECRET` (12 h expiry). Authorization is enforced on the
server in `SecurityConfig` and again in the services (students see only their own issues, staff only their assigned ones);
the React route guards only control what is shown. Uploaded files are type-checked by content and renamed. Password reset
links are returned in the API response in development mode only (`EXPOSE_RESET_LINK`); no email is sent.

## 9. Known limitations / future work

No email or SMS delivery; notifications are polled, not pushed; single admin role (no separate super-admin);
SLA values are demo defaults; the frontend is not localised; no automated frontend tests.

See `docs/API-Documentation.md` and `docs/Database-Design.md` for details.
