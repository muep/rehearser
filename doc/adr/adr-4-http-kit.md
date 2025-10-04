# ADR-4 Use http-kit for HTTP serving

## Context
The backend requires a lightweight, performant HTTP server. Options
include Ring adapters like **http-kit** or **Jetty**. The project
prioritizes simplicity, familiarity, and compatibility with Ring
middleware.

## Decision
Adopt **http-kit** as the HTTP server for its balance of performance,
simplicity and familiarity.

## Expected Consequences

**Familiarity**: The team is already familiar with http-kit, which
will help them be productive with the codebase.

**Performance**: http-kit’s async I/O model efficiently handles
concurrent requests with low overhead, easily filling the performance
requirements of the expected workload.

**Simplicity**: Minimal setup and Ring compatibility reduce
boilerplate. The server integrates seamlessly with existing Clojure
web stacks (e.g., Compojure, Pedestal).

**Stability**: Actively maintained with a proven track record in
production. Fewer moving parts than full-fledged servlet containers
(e.g., Jetty).

**Limitations**: Lacks some advanced features, such as TLS. Some kind
of a HTTPS server will be needed in front of the application in
production.

## Candidate Comparison

1. **http-kit (chosen)**
   - **Pros**:
     - Lightweight, async by default.
     - Simple API; Ring-compatible.
     - Good community adoption (e.g., used in Pedestal).
     - Familiarity within the team
   - **Cons**:
     - No built-in TLS support.
     - Fewer features than servlet containers.

2. **Jetty**
   - **Pros**:
     - Feature-rich (HTTP/2, WebSockets, servlet spec).
     - Battle-tested in large-scale deployments.
   - **Cons**:
     - Heavier; more complex configuration.
     - Async support requires explicit setup.
