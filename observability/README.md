# Observability Stack

SmartLift monitoring stack includes:

- `Prometheus` for metrics collection and alert evaluation
- `Alertmanager` for alert routing and grouping
- `Grafana` for dashboards and alert visibility
- `node-exporter` for host-level metrics

## Start

```bash
docker compose up -d alertmanager prometheus grafana node-exporter
```

Core endpoints:

- Prometheus: `http://localhost:9091`
- Alertmanager: `http://localhost:9093`
- Grafana: `http://localhost:3001`

Grafana credentials:

- username: `admin`
- admin credentials: values from your environment or deployment secret

## Provisioned Components

- Prometheus datasource
- Alertmanager datasource
- SmartLift dashboard from `observability/grafana/dashboards/smartlift-overview.json`
- SLI/SLO definition in `observability/SLI_SLO.md`

## Alerts

Defined in `observability/prometheus/alert_rules.yml`:

- `Watchdog`
- `SmartLiftBackendDown`
- `SmartLiftApiAvailabilitySLOBreach`
- `SmartLiftApiLatencySLOBreach`

## Quick Verification

1. Check active Alertmanager alerts:

```bash
curl http://localhost:9093/api/v2/alerts
```

Expected result: active `Watchdog` alert.

2. Check Prometheus rules:

```bash
curl http://localhost:9091/api/v1/rules
```

Expected result: `Watchdog` state is `firing`.

3. Open Grafana and confirm both datasources exist:

- `Prometheus`
- `Alertmanager`

## Demo Trigger

To demonstrate a real failure alert, stop the backend container:

```bash
docker compose stop backend
```

After roughly 1 minute, `SmartLiftBackendDown` should fire and appear in:

- Prometheus rules
- Alertmanager active alerts
- Grafana Alertmanager datasource view

Restore the backend afterwards:

```bash
docker compose start backend
```
