# SmartLift SLI/SLO

This document defines the service-level indicators and objectives for the SmartLift business API.

Scope of the API included in SLI calculations:

- `/api/auth.*`
- `/api/lifts.*`
- `/api/maintenances.*`
- `/api/events.*`

Prometheus job:

- `smartlift-backend`

## Service Definition

For final delivery, SmartLift is treated as a single backend service that serves:

- authentication
- lift registry operations
- maintenance workflow operations
- event logging operations

The frontend is not the SLO target. The backend business API is the primary user-facing service for measurement.

## SLI 1: Availability

Definition:

- percentage of API requests that do not end with `5xx`

Objective:

- `99.5%` successful requests over a rolling `30d` window

Error budget:

- `0.5%` failed requests over `30d`
- equivalent uptime-style interpretation:
  - about `3h 36m` unavailable-equivalent budget per 30 days

Primary PromQL:

```promql
100 *
(
  sum(rate(http_server_requests_seconds_count{
    job="smartlift-backend",
    uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*",
    status!~"5.."
  }[30d]))
  /
  clamp_min(
    sum(rate(http_server_requests_seconds_count{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[30d])),
    0.0001
  )
)
```

Short-window operational view used in dashboard and alerting:

```promql
100 *
(
  sum(rate(http_server_requests_seconds_count{
    job="smartlift-backend",
    uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*",
    status!~"5.."
  }[5m]))
  /
  clamp_min(
    sum(rate(http_server_requests_seconds_count{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[5m])),
    0.0001
  )
)
```

Mapped alert:

- `SmartLiftApiAvailabilitySLOBreach`

## SLI 2: Latency

Definition:

- percentage of API requests completed within `500 ms`

Objective:

- at least `95%` of requests complete within `500 ms` over a rolling `30d` window

Primary PromQL:

```promql
100 *
(
  sum(rate(http_server_requests_seconds_bucket{
    job="smartlift-backend",
    uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*",
    le="0.5"
  }[30d]))
  /
  clamp_min(
    sum(rate(http_server_requests_seconds_count{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[30d])),
    0.0001
  )
)
```

Operational p95 view:

```promql
1000 *
histogram_quantile(
  0.95,
  sum by (le) (
    rate(http_server_requests_seconds_bucket{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[5m])
  )
)
```

Mapped alert:

- `SmartLiftApiLatencySLOBreach`

## SLI 3: Error Rate

Definition:

- percentage of API requests returning `5xx`

Objective:

- keep `5xx` rate below `0.5%` over a rolling `30d` window

Primary PromQL:

```promql
100 *
(
  sum(rate(http_server_requests_seconds_count{
    job="smartlift-backend",
    uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*",
    status=~"5.."
  }[30d]))
  /
  clamp_min(
    sum(rate(http_server_requests_seconds_count{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[30d])),
    0.0001
  )
)
```

Operational dashboard query:

```promql
100 *
(
  sum(rate(http_server_requests_seconds_count{
    job="smartlift-backend",
    uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*",
    status=~"5.."
  }[5m]))
  /
  clamp_min(
    sum(rate(http_server_requests_seconds_count{
      job="smartlift-backend",
      uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
    }[5m])),
    0.0001
  )
)
```

Note:

- this SLI is complementary to availability
- availability is the top-level SLO
- error rate is kept separately because it is easier to explain during demo and incident review

## SLI 4: Throughput

Definition:

- request volume in requests per second across business endpoints

Objective:

- no hard SLO target
- used as a capacity and load-test reference metric

PromQL:

```promql
sum(rate(http_server_requests_seconds_count{
  job="smartlift-backend",
  uri=~"/api/auth.*|/api/lifts.*|/api/maintenances.*|/api/events.*"
}[5m]))
```

Use cases:

- compare baseline traffic and stress traffic
- correlate latency and error growth under load
- define autoscaling thresholds later

## Saturation Indicators

These are not user-facing SLIs, but they are required for SRE analysis and autoscaling decisions.

### CPU

```promql
100 * system_cpu_usage{job="smartlift-backend"}
```

### JVM Heap

```promql
sum(jvm_memory_used_bytes{job="smartlift-backend",area="heap"})
```

### HikariCP

```promql
hikaricp_connections_active{job="smartlift-backend"}
hikaricp_connections_idle{job="smartlift-backend"}
hikaricp_connections_pending{job="smartlift-backend"}
```

Operational interpretation:

- rising CPU with rising latency indicates compute saturation
- rising heap with GC pressure indicates memory saturation risk
- non-zero pending Hikari connections indicates database pool pressure

## Alert Policy

Current implemented alerts:

- `SmartLiftBackendDown`
- `SmartLiftApiAvailabilitySLOBreach`
- `SmartLiftApiLatencySLOBreach`
- `Watchdog`

Recommended severity model:

- `critical`: backend unavailable
- `warning`: SLO degradation
- `none`: watchdog test alert

## Dashboard Mapping

The existing Grafana dashboard already contains the required operational views:

- backend up
- traffic
- error rate
- p95 latency
- throughput by URI
- 1h availability score
- 1h latency compliance
- CPU
- JVM heap
- HikariCP pool state

Source dashboard:

- `observability/grafana/dashboards/smartlift-overview.json`

## Final Demo Narrative

For the final demo, the monitoring story should be presented in this order:

1. Show normal state in Grafana.
2. Show `Watchdog` active in Alertmanager to prove the pipeline is alive.
3. Trigger backend outage or induced failure.
4. Show `SmartLiftBackendDown` firing.
5. During load test, show changes in:
   - throughput
   - p95 latency
   - error rate
   - CPU / Hikari saturation
6. Compare observed results against the SLO targets above.
