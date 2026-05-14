environment             = "dev"
kubeconfig_path         = "~/.kube/config"
kube_context            = "docker-desktop"
app_namespace           = "smartlift-dev"
observability_namespace = "smartlift-observability-dev"
frontend_ingress_host   = "smartlift-dev.local"

backend_image         = "ghcr.io/your-org/smartlift-backend:dev"
frontend_image        = "ghcr.io/your-org/smartlift-frontend:dev"
frontend_api_upstream = "http://smartlift-backend.smartlift-dev.svc.cluster.local:8080"
db_username           = "postgres"
db_password           = ""
jwt_secret            = ""
postgres_db           = "smartlift"

mail_enabled           = false
grafana_admin_password = ""
