# UCMS API Contract

**Base URL:** `/api`
**Auth:** Bearer JWT (all routes except `/auth/*`)

**Account gating:** Student accounts are **LIMITED** until they add + verify a real email.
- LIMITED students can login and view data.
- LIMITED students cannot create tickets (and therefore cannot upload attachments).

---

## Auth

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/login` | Public | Login with student ID + password |
| POST | `/api/auth/register` | Public | Register (enrolled students only) |
| POST | `/api/auth/forgot-password` | Public | Request password reset |

> **Note:** Password reset is handled entirely by the Android app via Supabase SDK. The backend does not expose a reset-password endpoint.

Notes:
- Registration uses `student_id` as the primary identifier. Format: `YYYYNNNN-C` (4-digit year + 4-digit student number + campus initial, e.g. `20230733-N`). No hyphen between year and number. Supabase Auth is used behind the scenes.
- Password reset requires a verified email. If the student has no verified email, return `400` with instructions to add/verify an email.

---

## Profile

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/users/me` | Student / Admin | Get own profile |
| PUT | `/api/users/me` | Student / Admin | Update name, course, year level |
| PUT | `/api/users/me/email` | Student / Admin | Add/update email; triggers verification; unlocks ticket creation when verified |

---

## Tickets

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/tickets` | Student | Create new concern ticket (requires verified email) |
| GET | `/api/tickets` | Student / Admin | Student: own tickets. Admin: all tickets (filter: status, category) |
| GET | `/api/tickets/{id}` | Student / Admin | Get ticket detail |
| PATCH | `/api/tickets/{id}/status` | Admin | Update status (PENDING → IN_PROGRESS → RESOLVED → CLOSED) |

Notes:
- If a student account is LIMITED (email not verified), `POST /api/tickets` must return `403 Forbidden`.

---

## Attachments

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/tickets/{id}/attachments` | Student | Upload photo/file → Supabase Storage; saves `storage_path` in DB |
| GET | `/api/tickets/{id}/attachments` | Student / Admin | List attachments for a ticket (includes signed URLs) |

Notes:
- Storage bucket is private; URLs returned by the API are signed and expire.
- `original_filename`, `mime_type`, and `size_bytes` are derived by the backend from the uploaded file — the client does not send these fields.

---

## Responses

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/tickets/{id}/responses` | Admin | Reply to a ticket |
| GET | `/api/tickets/{id}/responses` | Student / Admin | Get all responses for a ticket |

---

## Notifications

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/notifications` | Student | Get own notifications |
| PATCH | `/api/notifications/{id}/read` | Student | Mark one as read |
| PATCH | `/api/notifications/read-all` | Student | Mark all as read |

---

## Analytics

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/analytics/summary` | Admin | Total tickets, resolved %, unresolved count |
| GET | `/api/analytics/by-category` | Admin | Ticket count grouped by category |
| GET | `/api/analytics/unresolved` | Admin | List of unresolved tickets |

---

## Categories

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/categories` | Student / Admin | List all categories |
| POST | `/api/categories` | Admin | Create a new category |
| PUT | `/api/categories/{id}` | Admin | Update a category name |
| DELETE | `/api/categories/{id}` | Admin | Delete a category (only if no tickets reference it) |

Notes:
- Categories are admin-configurable. Default seed values: Academic-related, Grades, AIMS Problem, ID Concern, Facility Issue, Others.
- A category cannot be deleted if any ticket references it — return `409 Conflict`.
