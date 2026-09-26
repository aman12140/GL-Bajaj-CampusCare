# API documentation

Base URL: `http://localhost:8080`. All bodies are JSON unless stated. Authenticated calls send
`Authorization: Bearer <token>`. Roles are enforced in `SecurityConfig` and re-checked in the services.

Error format (all errors): `{ "success": false, "message": "...", "timestamp": "...", "status": 400, "errors": { "field": "message" } }`
(`errors` only for validation failures). Typical codes: 400 validation/illegal transition, 401 not logged in or deactivated,
403 wrong role/not your issue/password change required, 404, 409 duplicate.

Lists return `{ content, page, size, totalElements, totalPages }`; query params `page` (from 0) and `size`.

## Auth (public unless noted)
| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/register | Student only. `name, studentId, email, phone, course, branch, year, section, password, confirmPassword` |
| POST | /api/auth/login | `{email, password}` returns `{token, user}` |
| POST | /api/auth/forgot-password | `{email}` returns `{message, resetLink?}` (link only if `EXPOSE_RESET_LINK=true`) |
| POST | /api/auth/reset-password | `{token, newPassword}` |
| POST | /api/auth/change-password | Logged in. `{currentPassword, newPassword}` |
| GET | /api/auth/me | Current profile |
| GET, PUT | /api/profile | Own profile. PUT: `name, phone` (+ `course, branch, year, section` for students) |

## Issues
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | /api/issues | ADMIN | Filters below |
| POST | /api/issues | STUDENT | Form fields `title, categoryId, locationId, priority, description, contactNumber?, roomNumber?` (free text, max 40: letters, digits, space, - / .) + optional file `photo` |
| POST | /api/issues/suggest-priority | any | `{title, description, categoryId?}` returns `{priority, reasons[], note}` |
| GET | /api/issues/{id} | owner / assigned staff / admin | Includes `timeline`, `feedback`, `slaHours`, `dueAt`, `overdue` |
| PUT | /api/issues/{id} | student (own, while REPORTED) / admin | JSON edit |
| PUT | /api/issues/{id}/assign | ADMIN | `{staffId, comment?}`; staff must be ACTIVE |
| PUT | /api/issues/{id}/status | STAFF (assigned) / ADMIN | Form fields `status` (IN_PROGRESS, RESOLVED, REJECTED), `comment` (required for RESOLVED and REJECTED), optional `photo` |
| PUT | /api/issues/{id}/confirm | STUDENT (owner) | RESOLVED to CLOSED |
| PUT | /api/issues/{id}/reopen | STUDENT (owner) | `{reason}` (required) |
| POST | /api/issues/{id}/feedback | STUDENT (owner) | `{rating 1-5, comment?}`; one per issue |

List filters (query params, all optional): `q, status, group (PENDING | IN_PROGRESS | DONE), priority, categoryId,
departmentId, locationId, staffId, range (TODAY | WEEK | MONTH | QUARTER | CUSTOM), fromDate, toDate (yyyy-MM-dd)`.

Role lists: `GET /api/student/issues` (own issues), `GET /api/staff/issues` (assigned to me). Dashboards:
`GET /api/student/dashboard`, `GET /api/staff/dashboard`.

## Admin
| Method | Path | Notes |
|---|---|---|
| GET | /api/admin/dashboard, /api/admin/analytics | Same filters as the issue list; real aggregates |
| GET | /api/admin/export/csv, /api/admin/export/pdf | Same filters; file download |
| GET | /api/admin/staff | `departmentId, status, q` |
| POST | /api/admin/staff | `name, employeeId, email, phone, departmentId, designation, temporaryPassword` |
| PUT | /api/admin/staff/{id} | Same fields except password |
| PUT | /api/admin/staff/{id}/deactivate, /activate | Never deletes |

## Master data (GET: any logged-in user; changes: ADMIN)
`/api/departments`, `/api/categories`, `/api/locations`, `/api/location-types`: GET (optional `activeOnly=true`), POST,
`PUT /{id}` (not for location types), `PUT /{id}/status` with `{status: "ACTIVE" | "INACTIVE"}`.
Category body: `{name, description?, parentId?, departmentId?}`. Location body: `{name, level (BUILDING | FLOOR | ROOM), type?, parentId?, floor?}`.

## Notifications (logged in)
`GET /api/notifications`, `GET /api/notifications/unread-count` (`{count}`), `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`.

Uploaded files are served at `/uploads/...`.
