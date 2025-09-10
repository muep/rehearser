# Rehearser MVP

MVP Requirements in brief

## Project description ##

A web application to let users track, what tunes they have
practiced. The application provides insights into the practicing
history and gives the user suggestions on what to practice next.

## Functional requirements ##
### Users ###

There are two kinds of users:
1. The normal user - later just User. The one who wants to track their
   practice sessions and repertoire.
2. The administrator, or later just Admin. Likely to be just one
   person.

### User interface ###

The service is a web application, so it needs to be usable with a
typical browser, such as Firefox or Chrome. The user interface must
behave sensibly both in a mobile form factor and on full-sized
computers.

### Use cases for Users ###
#### Registration and password reset ####

Users must be able to sign up to use the service themselves. There are
two acceptable approaches to this:

1. Have local, application-specific user accounts and allow users to
   self-register with an email address.
2. Integrate with external identity providers and allow users to use
   e.g. their accounts at Google or Microsoft

The choice between the two main approaches should come down to
whichever turns out to be a smaller undertaking.

#### Tune collection ####

Users must be able to maintain a collection of tunes they wish to
practice. This includes adding new entries, viewing the existing ones,
editing them or removing them.

For every tune, at least these fields exist:
- Name
- Description

#### Rehearsal entries ####

Users must be able to mark, when they have rehearsed a tune. The
rehearsal entry would have at least:

1. Reference to the tune that was practiced
2. Timestamp of the time when the rehearsal occurred
3. A remarks field that a user can use to record their own thoughts on
   what happened

The rehearsal entries must be at least removable. Preferably at least
the remarks would also be editable.

There must be a way for the user to view the rehearsal entries related
to a tune.

### Use cases for Admin ###

There are not many use cases supported for the Admin. Required,
though, are:

- View a list of user accounts

## Non-functional requirements ##

### Minimized environmental effects ###

The application is written at a time when use of natural resources on
computing is a growing. The application should be designed to minimize
its contribution to this as much as possible.

Among other things, this means that the application should minimize:

- Amount of purchased computing resources
- Network traffic
- Requirements on end-user devices

### Accessibility ###

Accessibility is important in today's online services. The application
is intended to eventually be accessible to people with disabilities.
This means that while the initial MVP does not have concrete goals on
this, it should avoid making choices that would make it difficult to reach
e.g. WCAG 2.1 AA compliance later.

### Data protection ###

The application will probably be available to EU citizens and thus
needs to be compliant with the GDPR. Data should reside in the EU.
It should also be structured so that it is easy to remove all data
related to a user if they request it.

### Simplicity ###

There are very limited human resources for development, so things
should stay as simple as possible.

There are plans for further features that could be added, but the
features in this document represent an MVP that should initially be
the focus.

It is expected, though, that the latter choice keeps the initial scope
smaller, so

### Testability ###

At least the basics of the application must be testable
with a rapid feedback loop. This probably requires automatic tests
that can be run locally.

Also for interactive testing and development, it is expected that
most, if not all, of the application needs to be runnable locally.

Dependencies on external services should be selected so, that there
either is a local substitute for them, or the external service can be
disabled for local development without breaking everything.

### Operation costs ###

Maximum budget for running the service on the public internet is
tight. An initially envisioned level is either:

- EUR 10 per month, or
- EUR 0.1 per active user per month

, depending on which one of these is higher.

This section intentionally does not propose any specific
infrastructure providers, to keep options open when searching for a
hosting solution.

### Hosting preferences ###

The tight hosting budget is expected to rule out some otherwise
reasonable choices for hosting the application. To ensure that
remaining options are not unnecessarily ruled out, preferences for
specific providers are not given here. There are still some general
preferences:

- Application data must reside in the EU
- On cost comparisons, any time-limited free tier offers should not be
  considered. Perpetual free tiers are acceptable.
- Since the application is otherwise small and simple in scope, it
  would be preferable to have smaller number of providers instead
  getting e.g. a database platform from one provider and backend
  hosting from somewhere else.

### Data transfer minimization ###

While the application will probably require an internet connection,
it should not transfer more data than necessary.

This serves many purposes:

- Environmental goals
- User experience on slow connections
- Minimization of hosting costs

Concrete targets in this area:
- Keep initial download of opening the site small, e.g. less than 100 KiB
- HTTP traffic to exchange data with the server should be designed
  to the use case, avoiding unnecessary round trips
