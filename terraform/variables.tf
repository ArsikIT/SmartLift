variable "kubeconfig_path" {
  description = "Path to the kubeconfig file used by the Kubernetes provider."
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "Kubernetes context name to use from kubeconfig."
  type        = string
  default     = ""
}

variable "environment" {
  description = "Environment name used in labels and outputs."
  type        = string
}

variable "app_namespace" {
  description = "Namespace for SmartLift application workloads."
  type        = string
  default     = "smartlift"
}

variable "observability_namespace" {
  description = "Namespace reserved for observability workloads."
  type        = string
  default     = "smartlift-observability"
}

variable "backend_image" {
  description = "Container image for SmartLift backend."
  type        = string
}

variable "backend_replicas" {
  description = "Initial number of backend replicas."
  type        = number
  default     = 2
}

variable "backend_min_replicas" {
  description = "Minimum replica count for backend HPA."
  type        = number
  default     = 2
}

variable "backend_max_replicas" {
  description = "Maximum replica count for backend HPA."
  type        = number
  default     = 6
}

variable "backend_hpa_target_cpu_utilization" {
  description = "Target average CPU utilization percentage for the backend HPA."
  type        = number
  default     = 70
}

variable "frontend_image" {
  description = "Container image for SmartLift frontend."
  type        = string
}

variable "frontend_replicas" {
  description = "Initial number of frontend replicas."
  type        = number
  default     = 1
}

variable "frontend_api_upstream" {
  description = "Upstream URL used by frontend nginx for /api proxying."
  type        = string
  default     = "http://smartlift-backend.smartlift.svc.cluster.local:8080"
}

variable "ingress_enabled" {
  description = "Whether to create an Ingress for SmartLift."
  type        = bool
  default     = true
}

variable "ingress_class_name" {
  description = "IngressClass name for SmartLift ingress."
  type        = string
  default     = "nginx"
}

variable "frontend_ingress_host" {
  description = "Host name served by the SmartLift frontend ingress."
  type        = string
  default     = "smartlift.local"
}

variable "db_url" {
  description = "Backend datasource JDBC URL."
  type        = string
  default     = ""
}

variable "db_username" {
  description = "Backend datasource username."
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "Backend datasource password."
  type        = string
  sensitive   = true
}

variable "postgres_db" {
  description = "PostgreSQL database name."
  type        = string
  default     = "smartlift"
}

variable "postgres_image" {
  description = "Container image for PostgreSQL."
  type        = string
  default     = "postgres:17-alpine"
}

variable "postgres_storage_size" {
  description = "Persistent volume size for PostgreSQL."
  type        = string
  default     = "10Gi"
}

variable "postgres_storage_class_name" {
  description = "StorageClass name for PostgreSQL PVC. Empty means cluster default."
  type        = string
  default     = ""
}

variable "postgres_cpu_request" {
  description = "CPU request for PostgreSQL."
  type        = string
  default     = "250m"
}

variable "postgres_cpu_limit" {
  description = "CPU limit for PostgreSQL."
  type        = string
  default     = "1"
}

variable "postgres_memory_request" {
  description = "Memory request for PostgreSQL."
  type        = string
  default     = "512Mi"
}

variable "postgres_memory_limit" {
  description = "Memory limit for PostgreSQL."
  type        = string
  default     = "1Gi"
}

variable "jwt_secret" {
  description = "JWT signing secret for SmartLift backend."
  type        = string
  sensitive   = true
}

variable "jwt_expiration" {
  description = "JWT expiration time in milliseconds."
  type        = string
  default     = "3600000"
}

variable "mail_enabled" {
  description = "Whether outbound mail delivery is enabled."
  type        = bool
  default     = false
}

variable "mail_from" {
  description = "Default sender for SmartLift email notifications."
  type        = string
  default     = "no-reply@smartlift.local"
}

variable "mail_host" {
  description = "SMTP host for outbound notifications."
  type        = string
  default     = ""
}

variable "mail_port" {
  description = "SMTP port for outbound notifications."
  type        = string
  default     = "587"
}

variable "mail_username" {
  description = "SMTP username for outbound notifications."
  type        = string
  default     = ""
  sensitive   = true
}

variable "mail_password" {
  description = "SMTP password for outbound notifications."
  type        = string
  default     = ""
  sensitive   = true
}

variable "backend_cpu_request" {
  description = "CPU request for backend pods."
  type        = string
  default     = "250m"
}

variable "backend_cpu_limit" {
  description = "CPU limit for backend pods."
  type        = string
  default     = "1"
}

variable "backend_memory_request" {
  description = "Memory request for backend pods."
  type        = string
  default     = "512Mi"
}

variable "backend_memory_limit" {
  description = "Memory limit for backend pods."
  type        = string
  default     = "1Gi"
}

variable "frontend_cpu_request" {
  description = "CPU request for frontend pods."
  type        = string
  default     = "100m"
}

variable "frontend_cpu_limit" {
  description = "CPU limit for frontend pods."
  type        = string
  default     = "250m"
}

variable "frontend_memory_request" {
  description = "Memory request for frontend pods."
  type        = string
  default     = "128Mi"
}

variable "frontend_memory_limit" {
  description = "Memory limit for frontend pods."
  type        = string
  default     = "256Mi"
}

variable "prometheus_image" {
  description = "Container image for Prometheus."
  type        = string
  default     = "prom/prometheus:v3.2.1"
}

variable "alertmanager_image" {
  description = "Container image for Alertmanager."
  type        = string
  default     = "prom/alertmanager:v0.28.1"
}

variable "grafana_image" {
  description = "Container image for Grafana."
  type        = string
  default     = "grafana/grafana:11.6.0"
}

variable "node_exporter_image" {
  description = "Container image for node-exporter."
  type        = string
  default     = "prom/node-exporter:v1.8.2"
}

variable "grafana_admin_user" {
  description = "Grafana admin username."
  type        = string
  default     = "admin"
}

variable "grafana_admin_password" {
  description = "Grafana admin password."
  type        = string
  sensitive   = true
  default     = "admin"
}

variable "prometheus_cpu_request" {
  description = "CPU request for Prometheus."
  type        = string
  default     = "100m"
}

variable "prometheus_cpu_limit" {
  description = "CPU limit for Prometheus."
  type        = string
  default     = "500m"
}

variable "prometheus_memory_request" {
  description = "Memory request for Prometheus."
  type        = string
  default     = "256Mi"
}

variable "prometheus_memory_limit" {
  description = "Memory limit for Prometheus."
  type        = string
  default     = "512Mi"
}

variable "alertmanager_cpu_request" {
  description = "CPU request for Alertmanager."
  type        = string
  default     = "50m"
}

variable "alertmanager_cpu_limit" {
  description = "CPU limit for Alertmanager."
  type        = string
  default     = "250m"
}

variable "alertmanager_memory_request" {
  description = "Memory request for Alertmanager."
  type        = string
  default     = "128Mi"
}

variable "alertmanager_memory_limit" {
  description = "Memory limit for Alertmanager."
  type        = string
  default     = "256Mi"
}

variable "grafana_cpu_request" {
  description = "CPU request for Grafana."
  type        = string
  default     = "100m"
}

variable "grafana_cpu_limit" {
  description = "CPU limit for Grafana."
  type        = string
  default     = "500m"
}

variable "grafana_memory_request" {
  description = "Memory request for Grafana."
  type        = string
  default     = "256Mi"
}

variable "grafana_memory_limit" {
  description = "Memory limit for Grafana."
  type        = string
  default     = "512Mi"
}

variable "node_exporter_cpu_request" {
  description = "CPU request for node-exporter."
  type        = string
  default     = "50m"
}

variable "node_exporter_cpu_limit" {
  description = "CPU limit for node-exporter."
  type        = string
  default     = "200m"
}

variable "node_exporter_memory_request" {
  description = "Memory request for node-exporter."
  type        = string
  default     = "64Mi"
}

variable "node_exporter_memory_limit" {
  description = "Memory limit for node-exporter."
  type        = string
  default     = "128Mi"
}
