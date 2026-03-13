# UCMS Android

University Concern Management System — Android mobile client (Java + XML).

[`backend repo`](https://github.com/ikennot/ucms-backend)
---

## Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Hedgehog or later |
| JDK | 11+ |
| Android SDK | API 24 (Android 7.0) minimum |
| Git | Any recent version |

---

## Clone the Repository

```bash
git clone git@github.com:XpnsiveSharks/ucms-android.git
cd ucms-android
```

---

## Local Configuration

The app reads sensitive config from `local.properties` (gitignored — never committed).

After cloning, add the following to your `local.properties` file (in the root of the project):

```properties
SUPABASE_ANON_KEY=<ask the project lead for this value>
BACKEND_BASE_URL=http://10.0.2.2:8080/
```

> ⚠️ `local.properties` must never be committed. It is already in `.gitignore`.
> ⚠️ `BACKEND_BASE_URL` is the local emulator URL. If running on a physical device, see [`docs/dev-setup.md`](docs/dev-setup.md) for instructions.

---

## Setup

1. Open **Android Studio**
2. Select **Open** → navigate to the cloned `ucms-android` folder
3. Wait for Gradle sync to complete
4. Once sync is done, you're ready to run

---

## Running the App

### Emulator
- Open **Device Manager** in Android Studio
- Create a virtual device (API 24+)
- Click **Run ▶**

### Physical Device (Wireless)
1. Enable **Developer Options** on your phone (tap Build Number 7x)
2. Enable **Wireless Debugging** under Developer Options
3. Android Studio → device dropdown → **Pair Devices Using Wi-Fi**
4. Scan the QR code from your phone
5. Select your device → Click **Run ▶**

> Make sure your phone and PC are on the same Wi-Fi network.

---

## Documentation

| Doc | Path | Description |
|-----|------|-------------|
| Design System | [`docs/design-system.md`](docs/design-system.md) | Colors, dimensions, theme setup, and layout guidelines — **read before touching any layout or resource file** |
| API Contract | [`docs/api-contract.md`](docs/api-contract.md) | All backend endpoints, request/response shapes |
| Dev Setup | [`docs/dev-setup.md`](docs/dev-setup.md) | Local configuration, backend URL setup for emulator and real device |
| Roles & Permissions | [`docs/roles-permissions.md`](docs/roles-permissions.md) | Student vs Admin rules, ownership checks |
| Ticket Status Flow | [`docs/ticket-status-flow.md`](docs/ticket-status-flow.md) | Valid ticket status transitions |

---

## Notes

- Min SDK: **API 24** (Android 7.0)
- Target SDK: **API 36**
- Backend: Spring Boot REST API (see backend repo for setup)
