# UCMS Android — Design System

> Single source of truth for colors, dimensions, and theme configuration.
> All values are defined in `res/values/` — never hardcode in layouts.

---

## Color Palette

Defined in `app/src/main/res/values/colors.xml`.

### Brand — Primary (Orange)

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorPrimary` | `#FFAA33` | Buttons, active nav icon, avatar background, links, status bar |
| `@color/colorPrimaryDark` | `#F78914` | Accents, pressed states, borders |
| `@color/colorOnPrimary` | `#FFFFFF` | Text/icons on top of primary color (e.g. button label) |
| `@color/colorPrimaryContainer` | `#FFE0A0` | Light orange container backgrounds |
| `@color/colorOnPrimaryContainer` | `#1A1A1A` | Text on primary container |

### Brand — Secondary (Green)

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorSecondary` | `#92CD28` | Resolved status badge, progress bar fill, total tickets card |
| `@color/colorSecondaryLight` | `#B6EE56` | In-Progress status badge, chart bars |
| `@color/colorSecondaryLighter` | `#E4FF7F` | Subtle highlights, backgrounds |
| `@color/colorOnSecondary` | `#FFFFFF` | Text/icons on secondary color |
| `@color/colorSecondaryContainer` | `#D4F5A0` | Light green container backgrounds |
| `@color/colorOnSecondaryContainer` | `#1A1A1A` | Text on secondary container |

### Surface & Background

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorBackground` | `#FFFFFF` | Screen background |
| `@color/colorSurface` | `#F5F5F5` | Card backgrounds, bottom sheets |
| `@color/colorOnSurface` | `#1A1A1A` | Text/icons on surface |
| `@color/colorOnBackground` | `#1A1A1A` | Text/icons on background |

### Text

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorTextPrimary` | `#1A1A1A` | Primary text (titles, body) |
| `@color/colorTextSecondary` | `#757575` | Secondary text (subtitles, hints, timestamps) |

### Divider & Border

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorDivider` | `#E0E0E0` | Dividers, card borders, separators |

### Status Badges

| Token | Hex | Usage |
|-------|-----|-------|
| `@color/colorStatusPending` | `#FFAA33` | Pending ticket badge |
| `@color/colorStatusInProgress` | `#B6EE56` | In-Progress ticket badge |
| `@color/colorStatusResolved` | `#92CD28` | Resolved ticket badge |
| `@color/colorStatusClosed` | `#757575` | Closed ticket badge |

---

## Dimensions

Defined in `app/src/main/res/values/dimens.xml`.

### Spacing

| Token | Value | Usage |
|-------|-------|-------|
| `@dimen/screen_padding` | `24dp` | Root layout padding on all screens |
| `@dimen/spacing_xxlarge` | `32dp` | Title → first input margin |
| `@dimen/spacing_xlarge` | `24dp` | Section top margin |
| `@dimen/spacing_large` | `20dp` | Button / primary input top margin |
| `@dimen/spacing_medium` | `16dp` | Secondary button / link top margin |
| `@dimen/spacing_small` | `12dp` | Description / secondary text top margin |
| `@dimen/spacing_xsmall` | `10dp` | Tight spacing between form fields |

### Corner Radius

| Token | Value | Usage |
|-------|-------|-------|
| `@dimen/corner_radius_button` | `50dp` | All `MaterialButton` — pill shape |
| `@dimen/corner_radius_card` | `16dp` | Cards, list items, containers |
| `@dimen/corner_radius_badge` | `50dp` | Status badges, chips |
| `@dimen/corner_radius_input` | `8dp` | Input fields (if custom shape needed) |

---

## Theme

Defined in `app/src/main/res/values/themes.xml`.  
Parent: `Theme.Material3.Light.NoActionBar`

### Material3 Color Slot Mapping

| Material3 Slot | Maps To | Effect |
|----------------|---------|--------|
| `colorPrimary` | `@color/colorPrimary` | Buttons, active nav, FAB, focus rings |
| `colorOnPrimary` | `@color/colorOnPrimary` | Button text/icons |
| `colorPrimaryContainer` | `@color/colorPrimaryContainer` | Tonal button backgrounds |
| `colorOnPrimaryContainer` | `@color/colorOnPrimaryContainer` | Text on tonal buttons |
| `colorSecondary` | `@color/colorSecondary` | Chips, toggles, secondary actions |
| `colorOnSecondary` | `@color/colorOnSecondary` | Text on secondary elements |
| `colorSecondaryContainer` | `@color/colorSecondaryContainer` | Secondary tonal backgrounds |
| `colorOnSecondaryContainer` | `@color/colorOnSecondaryContainer` | Text on secondary tonal |
| `colorSurface` | `@color/colorBackground` | Card/sheet backgrounds |
| `colorOnSurface` | `@color/colorTextPrimary` | Text on cards/sheets |
| `colorOnBackground` | `@color/colorTextPrimary` | General text color |
| `android:colorBackground` | `@color/colorBackground` | Screen background |
| `android:statusBarColor` | `@color/colorPrimary` | Status bar (orange) |
| `android:textColorPrimary` | `@color/colorTextPrimary` | Default text color |
| `android:textColorSecondary` | `@color/colorTextSecondary` | Secondary/hint text color |

### Notes
- Dark mode is **disabled** — `values-night/themes.xml` uses the same light theme
- Do not override `colorPrimary` or text colors directly in layouts — use theme attributes (`?attr/colorPrimary`)

---

## Usage Rules

1. **Never hardcode colors** — always use `@color/` tokens
2. **Never hardcode dimensions** — always use `@dimen/` tokens
3. **Never hardcode strings** — always use `@string/` references
4. **Buttons** — always add `app:cornerRadius="@dimen/corner_radius_button"` for pill shape
5. **Cards** — always use `app:cardCornerRadius="@dimen/corner_radius_card"` on `MaterialCardView`
6. **Status badges** — use `@color/colorStatus*` tokens, never inline hex
7. **Text colors** — use `?attr/colorPrimary` for links/active, `@color/colorTextSecondary` for hints

---

## File Locations

| File | Path |
|------|------|
| Colors | `app/src/main/res/values/colors.xml` |
| Dimensions | `app/src/main/res/values/dimens.xml` |
| Strings | `app/src/main/res/values/strings.xml` |
| Theme (light) | `app/src/main/res/values/themes.xml` |
| Theme (night) | `app/src/main/res/values-night/themes.xml` |

---

## Layout Guidelines

> These rules apply to all XML layout files. Follow them before submitting a PR.

### 1. No Hardcoded Values
- ❌ `android:padding="24dp"` → ✅ `android:padding="@dimen/screen_padding"`
- ❌ `android:textColor="#1A1A1A"` → ✅ `android:textColor="@color/colorTextPrimary"`
- ❌ `android:text="Login"` → ✅ `android:text="@string/login"`

### 2. View ID Naming Convention
Use `camelCase` with a type prefix:

| Prefix | View Type | Example |
|--------|-----------|---------|
| `tv` | TextView | `tvTitle`, `tvDescription` |
| `btn` | MaterialButton | `btnLogin`, `btnSubmit` |
| `et` | TextInputEditText | `etEmail`, `etPassword` |
| `til` | TextInputLayout | `tilEmail`, `tilPassword` |
| `rv` | RecyclerView | `rvTickets`, `rvNotifications` |
| `iv` | ImageView | `ivAvatar`, `ivAttachment` |
| `pb` | ProgressBar | `progressBar` |
| `cv` | MaterialCardView | `cvTicketItem`, `cvSummary` |

### 3. Layout Structure
- Use `ConstraintLayout` as root — avoid nested `LinearLayout`/`RelativeLayout`
- Use `0dp` (match constraint) for width/height when constrained on both sides
- Keep hierarchy flat — max 3 levels deep

### 4. Buttons
- Always use `MaterialButton` — never plain `Button`
- Always add `app:cornerRadius="@dimen/corner_radius_button"` for pill shape
- Full-width buttons: `android:layout_width="0dp"` + constrained to parent

### 5. Cards
- Always use `MaterialCardView` — never plain `View` with background
- Always add `app:cardCornerRadius="@dimen/corner_radius_card"`
- Add `app:cardElevation="2dp"` for subtle shadow

### 6. Text
- Always use `android:textAppearance` with Material3 styles:
  - Titles: `@style/TextAppearance.Material3.HeadlineMedium`
  - Body: `@style/TextAppearance.Material3.BodyLarge`
  - Captions: `@style/TextAppearance.Material3.BodySmall`
- Never set `android:textSize` directly — use `textAppearance`

### 7. Accessibility
- All `ImageView` must have `android:contentDescription`
- Clickable `TextView` must have `android:clickable="true"` + `android:focusable="true"`

### 8. Status Badges
Use `TextView` with background drawable + `@color/colorStatus*`:
- Pending → `@color/colorStatusPending`
- In-Progress → `@color/colorStatusInProgress`
- Resolved → `@color/colorStatusResolved`
- Closed → `@color/colorStatusClosed`
