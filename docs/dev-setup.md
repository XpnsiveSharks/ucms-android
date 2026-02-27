# UCMS Android — Local Development Setup

> This doc covers how to configure the app for local development and testing on a real device.

---

## Backend Base URL

### Emulator
- Use `http://10.0.2.2:8080/` — this is the Android emulator's alias for the host machine's localhost.

### Real Device
- Your phone and development machine must be on the **same WiFi network**.
- Find your machine's local IP:
  ```bash
  hostname -I
  ```
  Use the first IP in the output (e.g. `192.168.1.9`).
- Set in `local.properties`:
  ```
  BACKEND_BASE_URL=http://<your-local-ip>:8080/
  ```
  Example:
  ```
  BACKEND_BASE_URL=http://192.168.1.9:8080/
  ```

> ⚠️ `local.properties` is gitignored — never commit it.

---

## local.properties Template

```
BACKEND_BASE_URL=http://10.0.2.2:8080/
SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

> Get `SUPABASE_ANON_KEY` from the project lead or Supabase dashboard.

---

## Notes
- Switch `BACKEND_BASE_URL` back to `http://10.0.2.2:8080/` when testing on emulator.
- Do not hardcode IPs in source files — always use `local.properties`.
