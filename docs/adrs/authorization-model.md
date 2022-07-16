Now that some privileged operations become available in the HTTP API,
this needs some thought.

The initial model is such that a client sending a request is one of:
- anonymous
- normal user
- admin

An anonymous user is basically one without a valid session.

Normal users are those who have a row in the `account` table. They
have an `account-id` to which their owned resources are connected. The
session of a normal user will look like
`{:account-id 1 :account-name "alice" :account-admin? false}`.

An admin user is not stored in the database at all. They have a single
password whose hash is provided through the environment variable. For
this user, there is a separate login route that sets up a session
looking like
`{:account-id nil :account-name "admin" :account-admin? true}`.
