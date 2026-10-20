# Security Specification & Test Payloads for MYRA Connect

## Data Invariants
1. A user can only access, list, create, update, or delete devices, permissions, commands, pairing sessions, and audit logs within their own `/users/{userId}` hierarchy where `request.auth.uid == userId`.
2. Cross-user access is mathematically blocked: User A (`uid_A`) cannot read or write any document in `/users/{uid_B}`.
3. Unauthenticated requests (`request.auth == null`) are completely rejected by the default-deny global net.
4. Device permissions and remote commands are strictly isolated per-device and per-user.
5. All document ID path parameters are validated using regex pattern `^[a-zA-Z0-9_\\-]+$` with length <= 128 chars to prevent ID poisoning attacks.

## The Dirty Dozen Rejected Payloads
1. **Unauthenticated Read:** `GET /users/user123/devices` with `request.auth = null` -> PERMISSION_DENIED.
2. **Cross-User Device Read:** `GET /users/victim456/devices/device1` as `attacker789` -> PERMISSION_DENIED.
3. **Cross-User Command Dispatch:** `POST /users/victim456/commands` as `attacker789` -> PERMISSION_DENIED.
4. **Cross-User Permission Modification:** `PATCH /users/victim456/permissions/dev1` as `attacker789` -> PERMISSION_DENIED.
5. **Path ID Injection:** `POST /users/user123/devices/$$$INVALID$$$` -> PERMISSION_DENIED.
6. **Fake User Creation:** `POST /users/victim456` with `incoming().uid != request.auth.uid` -> PERMISSION_DENIED.
7. **Unverified Token Write:** User with `email_verified == false` attempting privileged writes -> PERMISSION_DENIED.
8. **Malicious Giant Payload:** Device document exceeding maximum field length or boundary limits -> PERMISSION_DENIED.
9. **Shadow Field Injection:** Attempting to inject unrestricted root admin fields in user document -> PERMISSION_DENIED.
10. **State Shortcutting:** Forcing command status to `SUCCESS` without authorized device target matching -> PERMISSION_DENIED.
11. **Direct Root Document Scraping:** `GET /devices` or `GET /commands` outside of `/users/{userId}` -> PERMISSION_DENIED.
12. **Pairing Session Hijack:** Reading pairing sessions of another user -> PERMISSION_DENIED.
