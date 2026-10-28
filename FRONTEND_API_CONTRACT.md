# Frontend API contract

Base URL: `http://localhost:8080`. JSON endpoints use `Content-Type: application/json` and UTF-8. Authenticated endpoints require `Authorization: Bearer <JWT>` from the existing login route. The JWT principal is resolved to the persisted account; no `/api/users/me` request accepts an account ID.

## Shared error response

Except for successful binary/image responses, API errors use this JSON shape:

```json
{
  "timestamp": "2026-10-28T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/auth/public/verify-otp",
  "validationErrors": {
    "otp": "must match \"\\d{6}\""
  }
}
```

`validationErrors` is `{}` when no field-level validation applies. Authentication failures are `401`, authorization failures are `403`. Other per-route error statuses are listed below.

## Public registration and login

### `POST /api/auth/public/send-otp`

- **Authentication:** Public.
- **Request:** `application/json`, `SendOtpRequest`:
  ```json
  {"email":"person@example.com"}
  ```
- **Validation:** `email` is required and must be a valid email address.
- **Success:** `202 Accepted`, `ApiMessageResponse`:
  ```json
  {"message":"Verification code sent"}
  ```
- **Errors:** `400` invalid request, `409` email already registered, `503` configured SMTP delivery unavailable.
- **Behavior/persistence:** Generates a cryptographically random six-digit OTP, stores only its BCrypt hash in `otp_challenges`, and expires it after 10 minutes. SMTP sends the plaintext code to the supplied email. Newer codes supersede older outstanding codes.

### `POST /api/auth/public/verify-otp`

- **Authentication:** Public.
- **Request:** `application/json`, `VerifyOtpRequest`:
  ```json
  {"email":"person@example.com","otp":"123456"}
  ```
- **Validation:** Email is required and valid; OTP is exactly six decimal digits.
- **Success:** `200 OK`:
  ```json
  {"verified":true,"registrationToken":"<one-time-token>"}
  ```
- **Errors:** `400` invalid/expired code, more than five failed attempts, or request validation error.
- **Behavior/persistence:** Checks the latest unconsumed registration challenge, records successful verification, and counts failed attempts. On success, returns a cryptographically random one-time registration token and stores only its BCrypt hash with the verified challenge. The token must be supplied to complete registration and expires with the 10-minute OTP challenge.

### `POST /api/auth/public/complete-registration`

- **Authentication:** Public.
- **Request:** `application/json`, `CompleteRegistrationRequest`:
  ```json
  {"email":"person@example.com","username":"person_1","password":"correct-horse-42","registrationToken":"<one-time-token-from-verify-otp>"}
  ```
- **Validation:** Email required and valid; username is 3–50 characters matching `^[a-zA-Z0-9._-]+$`; password is 8–128 characters; `registrationToken` is required.
- **Success:** `201 Created`, `UserProfileResponse`:
  ```json
  {"id":42,"email":"person@example.com","username":"person_1","hasProfilePicture":false}
  ```
- **Errors:** `400` missing, invalid, expired, or already-consumed registration token or invalid request, `409` email/username already in use.
- **Behavior/persistence:** Requires the unexpired, verified challenge and matching one-time token issued by `verify-otp`. Only then it creates a `ROLE_USER` record with a BCrypt password hash and consumes the challenge in the same transaction. The challenge is locked while checked and consumed to prevent concurrent reuse. OTP and token are not sent again in this request.

There is no `POST /api/auth/public/register` route. Account creation is only available through this verified three-step flow; the prior direct registration handler and its service method have been removed.

### Existing login: `POST /api/auth/public/login`

- **Authentication:** Public.
- **Request:** `application/json`, existing `LoginRequest`:
  ```json
  {"username":"person_1","password":"correct-horse-42"}
  ```
- **Validation:** Existing request fields are `username` and `password`; both must be supplied for successful authentication.
- **Success:** `200 OK`, existing `JwtAuthenticationResponse`:
  ```json
  {"token":"<JWT>"}
  ```
- **Errors:** `400` malformed JSON, `401` invalid username/password.
- **Behavior/persistence:** Existing username/password authentication remains in place. The response field and endpoint are unchanged. Issued JWTs include the account ID for stable `/me` ownership while retaining username as the JWT subject; legacy tokens without that claim continue to resolve by username.

## Authenticated profile

All routes in this section require a valid Bearer JWT. The user account is identified from the authenticated JWT principal, not client input.

### `GET /api/users/me`

- **Request:** No body; no request `Content-Type` required.
- **Success:** `200 OK`, `UserProfileResponse`:
  ```json
  {"id":42,"email":"person@example.com","username":"person_1","hasProfilePicture":true}
  ```
- **Errors:** `401` missing/invalid JWT, `403` forbidden, `401` authenticated account no longer exists.
- **Validation/behavior/persistence:** No request fields. Reads the authenticated user's persisted ID, email, username, and whether a profile-picture storage key exists.

### `PATCH /api/users/me`

- **Request:** `application/json`, `UpdateProfileRequest`:
  ```json
  {"username":"new_name"}
  ```
- **Validation:** Username is required, 3–50 characters, and matches `^[a-zA-Z0-9._-]+$`.
- **Success:** `200 OK`, updated `UserProfileResponse`.
- **Errors:** `400` validation error, `401` missing/invalid JWT, `403` forbidden, `409` username already taken, `401` authenticated account no longer exists.
- **Behavior/persistence:** Updates only the authenticated user's username. New JWTs identify the user by account ID, so a username change does not invalidate the current JWT.

### `POST /api/users/me/profile-picture`

- **Request:** `multipart/form-data`; exact file field name is `file`. Accepted file content types: `image/png`, `image/jpeg`, and `image/gif`.
- **Validation:** File is required, non-empty, at most 5 MB, and must decode as a valid image.
- **Success:** `200 OK`, `ApiMessageResponse`:
  ```json
  {"message":"Profile picture uploaded"}
  ```
- **Errors:** `400` missing/empty/invalid image, `401` missing/invalid JWT, `403` forbidden, `413` file too large, `415` unsupported media type, `500` storage failure.
- **Behavior/persistence:** Stores image bytes through the `ProfilePictureStorage` abstraction, replaces the previous stored file, and persists only the returned storage key in MySQL (`users.profile_picture_key`).

### `GET /api/users/me/profile-picture`

- **Request:** No body; no request `Content-Type` required.
- **Success:** `200 OK`, raw image bytes with the stored `image/png`, `image/jpeg`, or `image/gif` response `Content-Type`; cache control is `no-store`.
- **Errors:** `401` missing/invalid JWT, `403` forbidden, `404` no profile picture, `500` storage failure.
- **Validation/behavior/persistence:** No request fields. Resolves the authenticated user's storage key from MySQL and loads the binary content through `ProfilePictureStorage`.

### `DELETE /api/users/me/profile-picture`

- **Request:** No body; no request `Content-Type` required.
- **Success:** `204 No Content` (empty body).
- **Errors:** `401` missing/invalid JWT, `403` forbidden, `404` no profile picture, `500` storage failure.
- **Validation/behavior/persistence:** No request fields. Deletes the authenticated user's stored image and clears the MySQL storage key.

## Authenticated security and email change

### `PATCH /api/users/me/password`

- **Request:** `application/json`, `ChangePasswordRequest`:
  ```json
  {"currentPassword":"correct-horse-42","newPassword":"another-correct-horse-42"}
  ```
- **Validation:** Both fields are required; new password is 8–128 characters.
- **Success:** `200 OK`, `ApiMessageResponse`:
  ```json
  {"message":"Password changed successfully"}
  ```
- **Errors:** `400` invalid request, incorrect current password, or new password same as current; `401` missing/invalid JWT, `403` forbidden.
- **Behavior/persistence:** Verifies the current BCrypt hash, hashes the new password with BCrypt, and persists it for the authenticated account.

### `POST /api/users/me/email-change/send-otp`

- **Request:** `application/json`, `SendOtpRequest`:
  ```json
  {"email":"new-address@example.com"}
  ```
- **Validation:** Email is required and valid.
- **Success:** `202 Accepted`, `ApiMessageResponse`:
  ```json
  {"message":"Verification code sent"}
  ```
- **Errors:** `400` invalid request or same as current email, `401` missing/invalid JWT, `403` forbidden, `409` email already in use, `503` configured SMTP delivery unavailable.
- **Behavior/persistence:** Generates a six-digit OTP and stores only its BCrypt hash, email, purpose, expiry, attempt count, and verification state in `otp_challenges`; SMTP sends the code to the new address. Email remains unchanged until OTP verification succeeds.

### `POST /api/users/me/email-change/verify-otp`

- **Request:** `application/json`, `VerifyOtpRequest`:
  ```json
  {"email":"new-address@example.com","otp":"123456"}
  ```
- **Validation:** Email is required and valid; OTP is exactly six decimal digits.
- **Success:** `200 OK`:
  ```json
  {"email":"new-address@example.com","verified":true}
  ```
- **Errors:** `400` invalid/expired code, more than five failed attempts, or invalid request; `401` missing/invalid JWT, `403` forbidden, `409` email already in use.
- **Behavior/persistence:** Validates the latest email-change challenge and atomically updates the authenticated account's email while consuming that challenge.

## Storage and SMTP configuration

Profile-picture storage uses the `ProfilePictureStorage` interface. `app.storage.provider` (environment variable `PROFILE_PICTURE_STORAGE_PROVIDER`) selects the implementation; `local` is the current implementation and writes to `PROFILE_PICTURE_STORAGE_DIRECTORY` (default `./uploads/profile-pictures`). MySQL stores only the generated storage key. Selecting an unimplemented provider fails application startup rather than silently falling back.

OTP delivery uses configured SMTP. Set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, and optionally `OTP_MAIL_FROM` for the deployment. The local defaults target `localhost:25`; OTP send returns `503` if delivery cannot complete. OTP values are never returned by the API.
