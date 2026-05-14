# Load Tests

This directory contains the baseline load-testing setup for SmartLift.

Tooling:

- `Locust`

Scenario coverage:

- authentication login
- lift list and lift detail reads
- maintenance list reads
- event list reads
- service workflow writes:
  - create event
  - create maintenance
  - update maintenance

## Prerequisites

1. Start the application stack:

```bash
docker compose up -d postgres backend frontend prometheus grafana alertmanager node-exporter
```

2. Install Locust:

```bash
pip install -r load-tests/requirements.txt
```

## Run In UI Mode

```bash
locust -f load-tests/locustfile.py --host http://localhost:8080
```

Open:

- `http://localhost:8089`

Suggested first run:

- users: `20`
- spawn rate: `2`
- duration: `5m`

## Run Headless

```bash
locust -f load-tests/locustfile.py --host http://localhost:8080 --headless --users 20 --spawn-rate 2 --run-time 5m
```

## User Mix

The scenario uses three user groups:

- `AuthUser`
  - validates login path under load
- `ReadOnlyApiUser`
  - stresses read traffic on lifts, events, and maintenances
- `ServiceWorkflowUser`
  - stresses write traffic on events and maintenances

Relative weights:

- `AuthUser`: `1`
- `ReadOnlyApiUser`: `5`
- `ServiceWorkflowUser`: `3`

## Seed Behavior

At the start of a test run, the script automatically creates:

- one `MANUFACTURER` organization with an admin user
- one `SERVICE` organization with an admin user
- one `MANAGEMENT` organization with an admin user
- five seed lifts shared between those organizations

This avoids manual preparation before every run.

Important:

- `register` is rate-limited in the application
- the script only uses registration during one-time setup
- the actual load phase stresses `login`, `lifts`, `events`, and `maintenances`

## What To Watch In Grafana

During the run, track:

- traffic
- error rate
- p95 latency
- availability score
- CPU
- JVM heap
- HikariCP active and pending connections

Reference documents:

- `observability/SLI_SLO.md`
- `observability/README.md`

## Suggested Progression

1. Baseline run:
   - `20` users, `5m`
2. Medium run:
   - `50` users, `10m`
3. Stress run:
   - `100` users, `10m`
4. Breakpoint run:
   - increase users until latency or error-rate SLO is breached

## Expected Outcome

This setup is intended to provide:

- throughput growth curves
- latency degradation points
- error-rate behavior under write-heavy load
- evidence for future autoscaling thresholds
