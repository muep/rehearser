# ADR-1 Build a stand-alone web application

## Context
The project has strict cost constraints and limited development
resources. Cloud-native approaches (e.g., serverless, microservices)
often introduce complexity and operational overhead that may exceed
the MVP's simplicity goals.

## Decision
Build a **stand-alone web application** (monolithic or simple 2-tier) using:
- A single backend (e.g., Python/Flask, Node.js, or Go)
- A lightweight database hosted on the same VM (e.g. SQLite or PostgreSQL)
- Static frontend files served by the backend or a CDN

Deploy to a **single low-cost VM** (e.g., Hetzner Cloud, DigitalOcean
Droplet) or a shared hosting provider with EU data residency.

## Expected Consequences
Choosing to do a traditional stand-alone application gives us a
well-understood setup and a lot of flexibility with regards to where
to host the application.

Doing a stand-alone application will make it trivial to run the
application locally during development.

Since the application will be running permanently, it is likely
accruing some costs regardless of utilization. On the other hand,
within EU there are many service providers whose price level fits the
project budget, even with this consideration.

Running the application is likely to require running a virtual
machine, which will then require some ongoing maintenance on top of
maintenance of the application itself. Scaling the application -
especially horizontally, will also not be as trivial as with some
other setups.

## Candidate Comparison

1. **Stand-alone web app (chosen solution)**
   - **Pros**:
     - Meets budget constraints.
     - Simpler ops (no orchestration, fewer moving parts).
     - Faster to develop with limited resources.
     - High flexibility with regards to where to host the app
   - **Cons**:
     - Scaling requires manual intervention.

2. **Cloud-native (serverless + managed DB)**
   - **Pros**:
     - Auto-scaling, pay-per-use pricing *could* fit budget if traffic is very low.
   - **Cons**:
     - Higher complexity → slower development.
     - Risk of cost overruns (e.g., DB queries, API calls).
     - Vendor lock-in (e.g., AWS Lambda + RDS).

3. **Static site + BaaS (e.g., Firebase, Supabase)**
   - **Pros**:
     - Minimal backend code; fast frontend iteration.
   - **Cons**:
     - BaaS costs may exceed budget at scale.
     - Less control over data/data residency.
     - Vendor lock-in
