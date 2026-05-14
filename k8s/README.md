# Kubernetes Autoscaling

This directory contains the initial Kubernetes manifests for SmartLift backend autoscaling.

Included resources:

- `Namespace`
- `ConfigMap`
- `Secret` template
- `Deployment`
- `Service`
- `HorizontalPodAutoscaler`

Directory:

- `k8s/backend`

## Prerequisites

Required before applying:

- a Kubernetes cluster
- `metrics-server` installed in the cluster
- a reachable PostgreSQL instance
- a published backend image

## Files

- `k8s/backend/namespace.yaml`
- `k8s/backend/configmap.yaml`
- `k8s/backend/secret.example.yaml`
- `k8s/backend/deployment.yaml`
- `k8s/backend/service.yaml`
- `k8s/backend/hpa.yaml`
- `k8s/backend/kustomization.yaml`

## What To Adjust

Before deployment, update:

1. `image` in `deployment.yaml`
2. `DB_URL` in `configmap.yaml`
3. actual secrets based on `secret.example.yaml`

## Apply

Create a real secret first:

```bash
kubectl apply -f k8s/backend/namespace.yaml
kubectl apply -f k8s/backend/secret.example.yaml
kubectl apply -k k8s/backend
```

For real use, copy `secret.example.yaml` to a private manifest and replace placeholder values.

## Verify

Check deployment and HPA:

```bash
kubectl get pods -n smartlift
kubectl get svc -n smartlift
kubectl get hpa -n smartlift
kubectl describe hpa smartlift-backend -n smartlift
```

Check rollout:

```bash
kubectl rollout status deployment/smartlift-backend -n smartlift
```

## Autoscaling Target

The current HPA policy is:

- min replicas: `2`
- max replicas: `6`
- target CPU utilization: `70%`

Scale-up behavior:

- up to `100%` growth per minute
- or `2` pods per minute

Scale-down behavior:

- stabilization window: `300s`
- downscale limit: `50%` per minute

## Load-Test Validation

Use the Locust scenario from `load-tests/` to validate autoscaling:

1. Start with baseline traffic.
2. Increase users gradually.
3. Watch:
   - `kubectl get hpa -w -n smartlift`
   - Grafana traffic
   - p95 latency
   - error rate
   - CPU
4. Record the traffic level where new replicas appear.
5. Compare pre-scale and post-scale latency/error behavior against `observability/SLI_SLO.md`.
