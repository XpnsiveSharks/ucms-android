# UCMS Ticket Status Flow

## State Diagram

```
                  ┌─────────┐
   [Student       │         │
    submits]      │ PENDING │
   ─────────────► │         │
                  └────┬────┘
                       │ Admin reviews
                       │ PATCH /tickets/{id}/status
                       ▼
                  ┌───────────┐
                  │           │
                  │ IN_PROGRESS│
                  │           │
                  └─────┬─────┘
                        │ Admin resolves
                        │ PATCH /tickets/{id}/status
                        ▼
                  ┌──────────┐
                  │          │
                  │ RESOLVED │
                  │          │
                  └─────┬────┘
                        │ Student confirms resolved; Admin closes
                        │ PATCH /tickets/{id}/confirm-resolved (Student)
                        │ PATCH /tickets/{id}/status (Admin)
                        ▼
                  ┌────────┐
                  │        │
                  │ CLOSED │  ◄── terminal state
                  │        │
                  └────────┘
```

---

## States

| Status | Meaning |
|--------|---------|
| `PENDING` | Ticket submitted, awaiting admin action |
| `IN_PROGRESS` | Admin has acknowledged and is working on it |
| `RESOLVED` | Concern has been addressed |
| `CLOSED` | Ticket archived — no further action |

---

## Transition Rules

| From | To | Who | Condition |
|------|----|-----|-----------|
| — | `PENDING` | System (on create) | Student submits ticket |
| `PENDING` | `IN_PROGRESS` | Admin | Admin reviews and accepts |
| `IN_PROGRESS` | `RESOLVED` | Admin | Admin marks concern resolved |
| `RESOLVED` | `CLOSED` | Admin | Student has confirmed resolved (`confirmed_resolved = true`) |
| Any | ❌ | Student | Students cannot change status |

### Student Confirmation Flow
- When a ticket reaches `RESOLVED`, the student receives a notification asking them to confirm.
- Student calls `PATCH /api/tickets/{id}/confirm-resolved` to confirm.
- Only after `confirmed_resolved = true` can Admin transition to `CLOSED`.
- If Admin attempts `RESOLVED → CLOSED` before student confirms → reject with `409 Conflict`.

### Invalid transitions (reject with 400)
- `PENDING → RESOLVED` (must pass through `IN_PROGRESS`)
- `PENDING → CLOSED`
- `IN_PROGRESS → PENDING`
- `RESOLVED → IN_PROGRESS` (no regression)
- `CLOSED → any` (terminal — immutable)
- `RESOLVED → CLOSED` before student confirms (`confirmed_resolved = false`) → reject with `409 Conflict`

---

## Notification Triggers

| Transition | Notification to Student |
|------------|------------------------|
| Created → `PENDING` | "Your concern has been received." |
| `PENDING` → `IN_PROGRESS` | "Your concern is now being reviewed." |
| `IN_PROGRESS` → `RESOLVED` | "Your concern has been resolved. Please confirm if your issue has been addressed." |
| `RESOLVED` → `CLOSED` | "Your ticket has been closed." |
| Admin replies (any status) | "Admin has responded to your concern." |

---

## Enforcement

- Valid transitions enforced in **service layer** — reject invalid jumps with `400 Bad Request`
- `CLOSED` tickets are immutable — any PATCH returns `403 Forbidden`
- Notification created in DB on every valid transition (in-app only, no external push)
- Status field is a DB enum — invalid values rejected at schema level
