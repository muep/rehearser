# ADR-8 Use unix-style epoch timestamps in the HTTP API

## Context

Some of the business logic of the application will deal with durations
of time and timestamps referring to some point in time. These
timestamps need to be transferred between the frontend and the
backend.

## Decision

Whenever practical, when needing to transmit a timestamp over the
network, send an integer offset from 1970-01-01T0000Z in seconds. When
sending a duration, send that as in integer at one-second precision as
well.

## Expected consequences

Using plain integers is immediately obvious to encode and decode into
JSON, without requiring counterparts to agree on a specific structure
for a structured time.

The unix-style timestamp is familiar and easy to understand for the
development team.

Humans will needs some helper tools if they need to precisely map
on-the-wire timestamps into familiar calendar times.

## Candidate comparison

1. **Unix-style epoch timestamp in an integer**
   - **Pros**:
     - Simple to understand
     - Small and cheap to encode/decode
     - Little room for anomalies
   - **Cons**:
     - May need replacement if there arise use cases requiring
       timezones or structured calendar information
     - Not as immediately human-readable as a written-out calendar time

2. **ISO 8601**
   - **Pros**:
     - Easy for humans to read
   - **Cons**:
     - Several variants of the same format
     - Stringly typed
