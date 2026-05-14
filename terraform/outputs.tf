output "app_namespace" {
  description = "Namespace created for SmartLift application workloads."
  value       = kubernetes_namespace_v1.app.metadata[0].name
}

output "observability_namespace" {
  description = "Namespace reserved for observability workloads."
  value       = kubernetes_namespace_v1.observability.metadata[0].name
}

output "backend_deployment_name" {
  description = "Backend deployment managed by Terraform."
  value       = kubernetes_deployment_v1.backend.metadata[0].name
}

output "backend_service_name" {
  description = "Backend service managed by Terraform."
  value       = kubernetes_service_v1.backend.metadata[0].name
}

output "backend_hpa_name" {
  description = "Backend HPA managed by Terraform."
  value       = kubernetes_horizontal_pod_autoscaler_v2.backend.metadata[0].name
}

output "frontend_service_name" {
  description = "Frontend service managed by Terraform."
  value       = kubernetes_service_v1.frontend.metadata[0].name
}

output "postgres_service_name" {
  description = "PostgreSQL service managed by Terraform."
  value       = kubernetes_service_v1.postgres.metadata[0].name
}

output "frontend_ingress_host" {
  description = "Frontend ingress host when ingress is enabled."
  value       = var.ingress_enabled ? kubernetes_ingress_v1.frontend[0].spec[0].rule[0].host : null
}

output "prometheus_service_name" {
  description = "Prometheus service managed by Terraform."
  value       = kubernetes_service_v1.prometheus.metadata[0].name
}

output "grafana_service_name" {
  description = "Grafana service managed by Terraform."
  value       = kubernetes_service_v1.grafana.metadata[0].name
}

output "alertmanager_service_name" {
  description = "Alertmanager service managed by Terraform."
  value       = kubernetes_service_v1.alertmanager.metadata[0].name
}
