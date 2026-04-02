# UCMS Android Tasks

## Manual QA — Realtime + Sync Validation

- [ ] Realtime ticket status update (Admin → Student)
  - Login as Student on phone A, open Ticket List/Home.
  - Login as Admin on phone B, open same ticket.
  - Admin changes status (`PENDING -> IN_PROGRESS`).
  - Expected:
    - Student screen updates within a few seconds (no manual refresh).
    - Notification count/badge updates (if applicable).
    - Reopening app sections still shows updated status (cache persisted).

- [ ] Realtime admin response posted
  - Student keeps ticket detail/list open.
  - Admin posts a response on the same ticket.
  - Expected:
    - Student sees new response after realtime-triggered background sync.
    - New notification appears in notifications screen.
    - No duplicate entries after repeated events.

- [ ] Profile update propagation
  - Student opens profile/home screen.
  - Update profile fields (name/course/year).
  - Expected:
    - Updated profile appears immediately or after short sync.
    - Home header/avatar initials reflect new name.
    - Relaunch app keeps new values from cache.

- [ ] Disconnect / reconnect resilience
  - Keep student app open.
  - Turn off internet for 20–30 seconds.
  - Admin performs ticket update during outage.
  - Turn internet back on.
  - Expected:
    - SSE reconnects automatically.
    - Delta sync catches missed update.
    - Final state matches backend (no stale status).

- [ ] Background / resume behavior
  - Put student app in background.
  - Admin changes ticket status.
  - Resume student app.
  - Expected:
    - On resume, data refreshes via sync flow.
    - Latest ticket status is visible without force close.

- [ ] Warm cache login speed
  - Login once (cold start), then logout/login same user.
  - Expected:
    - Second login skips heavy loading (warm cache path).
    - Background sync still fetches latest changes.

- [ ] Role boundary checks
  - Student should not access admin-only data (analytics sync endpoint).
  - Admin should not call student-only notifications sync endpoint.
  - Expected:
    - Proper authorization behavior (403/denied where required).
    - No crashes on mobile; errors handled gracefully.
