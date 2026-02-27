# UCMS Android

University Concern Management System — Android mobile client (Java + XML).

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
| Design System | `docs/design-system.md` | Colors, dimensions, theme setup, and layout guidelines — **read before touching any layout or resource file** |
| API Contract | `docs/api-contract.md` | All backend endpoints, request/response shapes |
| Roles & Permissions | `docs/roles-permissions.md` | Student vs Admin rules, ownership checks |
| Ticket Status Flow | `docs/ticket-status-flow.md` | Valid ticket status transitions |

---

## Notes

- Min SDK: **API 24** (Android 7.0)
- Target SDK: **API 36**
- Backend: Spring Boot REST API (see backend repo for setup)
