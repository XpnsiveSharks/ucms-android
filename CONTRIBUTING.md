# Contributing to UCMS Android

## Branch Naming

Branch off `development`. Use the following prefixes with kebab-case:

```
feat/ticket-list-screen
fix/login-crash-on-empty-field
chore/update-retrofit
docs/setup-instructions
```

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/). Keep the subject under 72 characters, imperative mood.

```
feat: add ticket submission screen
fix: handle null response in TicketAdapter
chore: bump Retrofit to 2.9.0
refactor: move API calls out of Activity
test: verify login flow on API 24 emulator
docs: update setup steps in README
```

## Pull Requests

1. Branch off `development`
2. Open PR targeting `development`
3. Ensure the app builds and runs before requesting review

### PR Template

Use this format when opening a PR:

```
## Type of Change
- [ ] feat — new feature
- [ ] fix — bug fix
- [ ] chore — maintenance, dependencies, config
- [ ] docs — documentation only
- [ ] refactor — code change that neither fixes a bug nor adds a feature
- [ ] test — adding or updating tests

## Labels
<!-- Apply labels from: priority (p0–p3), type (feat/fix/security/chore/docs/test/refactor), scope (backend/android/infra) -->

## What Changed
<!-- Short description of what was changed -->

## Why
<!-- Motivation and context. What problem does this solve? -->

## How to Test
<!-- Steps to verify the change works correctly -->
1. 
2. 

## Related Issues
<!-- Link related issues: Closes #123 -->

## Screenshots
<!-- For UI changes, add screenshots of before/after -->

## Checklist
- [ ] Code follows the Google Java Style Guide
- [ ] No logic in Activities or Fragments — use ViewModels
- [ ] All API calls go through `ApiClient` — no direct Supabase calls
- [ ] JWT attached via `AuthInterceptor` — no manual Authorization headers
- [ ] `403 ACCOUNT_LIMITED` handled with email verification prompt
- [ ] No hardcoded dimensions — use `@dimen/` tokens
- [ ] No hardcoded colors — use `@color/` tokens
- [ ] No hardcoded strings — use `@string/` references
- [ ] View IDs follow `camelCase` type prefix convention (`tvTitle`, `btnLogin`, etc.)
- [ ] Buttons use `MaterialButton` + `app:cornerRadius="@dimen/corner_radius_button"`
- [ ] Cards use `MaterialCardView` + `app:cardCornerRadius="@dimen/corner_radius_card"`
- [ ] No secrets or hardcoded URLs committed
- [ ] App builds and runs on API 24+
- [ ] CI passes
```

## Code Style

> See `docs/design-system.md` for the full design system reference (colors, dimensions, theme, layout rules).

- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- XML layout IDs use `camelCase` with type prefix (e.g. `btnSubmit`, `tvTicketTitle`) — see `docs/design-system.md`
- Activities and Fragments use `PascalCase` (e.g. `TicketDetailActivity`, `LoginFragment`)

## Package Structure

```
com.ucms/
├── activities/
├── fragments/
├── adapters/
├── models/
├── network/        ← Retrofit interfaces and client setup
└── utils/
```

## Testing

Before pushing, verify the app runs correctly on:

- An emulator running **API 24 or higher**, or
- A physical device

Check that the affected screens and flows work end-to-end.

## Secrets

- Never commit `local.properties` or hardcode API base URLs in source files
- Define sensitive values as `BuildConfig` fields via `gradle.properties` (gitignored)
- `local.properties` is gitignored by default — keep it that way

## CI

All PRs to `development` must pass the **Android CI** workflow before merging.

- CI runs `./gradlew build` (unit tests included) automatically on every PR
- Do not merge if CI is red

## Labels

Apply labels when creating GitHub issues and PRs. Use one from each relevant group.

### Priority
| Label | When to use |
|---|---|
| `p0-critical` | Security vulnerabilities, data loss, app cannot start |
| `p1-high` | Broken functionality, must fix before next release |
| `p2-medium` | Degraded functionality, fix when possible |
| `p3-low` | Nice to have, no functional impact |

### Type
| Label | When to use |
|---|---|
| `feat` | New feature or screen |
| `fix` | Bug fix |
| `security` | Security-related fix or hardening |
| `chore` | Maintenance, dependencies, config |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `refactor` | Code change with no feature or fix |

### Scope
| Label | When to use |
|---|---|
| `backend` | Backend only |
| `android` | Android only |
| `infra` | CI/CD, GitHub Actions, secrets |

## Issue Template

Use this format when creating GitHub issues:

```
### Overview
<!-- A concise description of what this issue covers and why it's needed. -->

### Blocked on
<!-- List any issues that must be completed first. Remove this section if not blocked. -->

### Tasks
<!-- Checklist of concrete implementation steps. One task per line. -->
- [ ] 

### View IDs
<!-- List all XML view IDs introduced or used in this issue (Design issues only). -->

### References
<!-- Link to relevant docs, layouts, or related issues. -->
- 

### Acceptance Criteria
<!-- Define what "done" looks like. Each criterion must be independently verifiable. -->
- 
```

### Guidelines
- **Title format:** `[Phase X<letter>-D] Design: <Screen Name>` or `[Phase X<letter>-L] Logic: <Screen Name>`
- **Design before Logic** — always create and complete the Design issue before starting the Logic issue
- **One screen per issue** — do not mix unrelated screens
- **Design tasks** must list all XML layout files, view IDs, and drawable/style references
- **Logic tasks** must reference view IDs from the Design issue and API endpoints from docs/api-contract.md
- **Acceptance Criteria** must be testable on a device/emulator — include empty states, error states, and edge cases
- **Labels** — always apply priority + type + scope
