# Rehearser API Summary

## General
- **Auth**: Session cookie after `POST /api/login`
- **Data format**: JSON (unless noted)
- **Timestamps**: Unix epoch seconds (ADR‑8)
- **Durations**: Integer seconds, `null` if ongoing
- **Errors**: Non‑200 with JSON body `{error: "...", humanized: "..."}`

---

## Authentication & Accounts

| Method | Path          | Purpose           | Request                      | Response                 |
|--------|---------------|-------------------|------------------------------|--------------------------|
| POST   | `/api/signup` | Create account    | Form: `username`, `password` | 200 OK                   |
| POST   | `/api/login`  | Authenticate      | Form: `username`, `password` | Sets session cookie      |
| POST   | `/api/logout` | End session       | –                            | 200 OK                   |
| GET    | `/api/whoami` | Current user info | –                            | `{account-id, username}` |

---

## Exercises

| Method | Path                | Purpose         | Request                | Response                       |
|--------|---------------------|-----------------|------------------------|--------------------------------|
| GET    | `/api/exercise`     | List exercises  | –                      | `[ {id, title, description} ]` |
| POST   | `/api/exercise`     | Create exercise | `{title, description}` | `{id, ...}`                    |
| GET    | `/api/exercise/:id` | Get exercise    | –                      | `{id, title, description}`     |
| PUT    | `/api/exercise/:id` | Update exercise | `{title, description}` | 200 OK                         |
| DELETE | `/api/exercise/:id` | Delete exercise | –                      | 200 OK                         |

---

## Variants

| Method | Path               | Purpose        | Request                | Response                       |
|--------|--------------------|----------------|------------------------|--------------------------------|
| GET    | `/api/variant`     | List variants  | –                      | `[ {id, title, description} ]` |
| POST   | `/api/variant`     | Create variant | `{title, description}` | `{id, ...}`                    |
| GET    | `/api/variant/:id` | Get variant    | –                      | `{id, title, description}`     |
| PUT    | `/api/variant/:id` | Update variant | `{title, description}` | 200 OK                         |
| DELETE | `/api/variant/:id` | Delete variant | –                      | 200 OK                         |

---

## Rehearsals & Entries

| Method | Path                       | Purpose                                  | Request                                          | Response                                                                 |
|--------|----------------------------|------------------------------------------|--------------------------------------------------|--------------------------------------------------------------------------|
| GET    | `/api/rehearsal`           | List rehearsals for user                 | –                                                | `[ {id, start-time, duration, title, description, is-open} ]`            |
| POST   | `/api/rehearsal`           | Create rehearsal                         | `{start-time, title, description, duration?}`    | `{id, ...}`                                                              |
| GET    | `/api/rehearsal/:id`       | Get rehearsal + embedded entries         | –                                                | `{id, ..., entries:[...]}`                                               |
| PUT    | `/api/rehearsal/:id`       | Update rehearsal (title, desc, duration) | Partial JSON                                     | 200 OK                                                                   |
| DELETE | `/api/rehearsal/:id`       | Delete rehearsal (cascade entries)       | –                                                | 200 OK                                                                   |
| POST   | `/api/rehearsal/:id/entry` | Add entry to rehearsal                   | `{exercise-id, variant-id, entry-time, remarks}` | `{id, ...}`                                                              |
| GET    | `/api/rehearsal/:id/entry` | List entries (alt. view)                 | –                                                | `[ {id, exercise-id, exercise-title, variant-id, entry-time, remarks} ]` |
| PUT    | `/api/entry/:id`           | Update entry                             | `{remarks?, entry-time?}`                        | 200 OK                                                                   |
| DELETE | `/api/entry/:id`           | Delete entry                             | –                                                | 200 OK                                                                   |

---

## Admin

| Method | Path                  | Purpose                  |
|--------|-----------------------|--------------------------|
| POST   | `/api/admin-login`    | Admin login              |
| POST   | `/api/admin/reset-db` | Reset database           |
| GET    | `/api/admin/status`   | Admin status             |

---

## System / Diagnostics

| Method | Path             | Purpose                  |
|--------|------------------|--------------------------|
| GET    | `/api/sys-stat`  | System stats             |
| GET    | `/api/sys-summary` | System summary         |
| GET    | `/api/reqstat`   | Request stats            |
| GET    | `/health`        | Health check             |
| POST   | `/api/fail`      | Trigger failure (test)   |
| GET    | `/api/fail`      | Failure status           |
| POST   | `/api/params`    | Set params               |
| GET    | `/api/params`    | Get params               |

---

## Example: Rehearsal with Entries

```json
GET /api/rehearsal/1
{
  "id": 1,
  "start-time": 1760107800,
  "duration": 2000,
  "title": "Evening tune-up",
  "description": "Worked on reels",
  "is-open": false,
  "entries": [
    {
      "id": 10,
      "exercise-id": 5,
      "exercise-title": "Kerry Reel",
      "variant-id": 2,
      "entry-time": 1760109900,
      "remarks": "Much improved on this one"
    }
  ]
}
```
