environment             = "prod"
kubeconfig_path         = "~/.kube/config"
kube_context            = "production-context"
app_namespace           = "smartlift"
observability_namespace = "smartlift-observability"
frontend_ingress_host   = "smartlift.example.com"

backend_image  = "ghcr.io/your-org/smartlift-backend:latest"
frontend_image = "ghcr.io/your-org/smartlift-frontend:latest"
db_username    = "postgres"
db_password    = ""
jwt_secret     = ""
postgres_db    = "smartlift"

mail_enabled           = false
grafana_admin_password = ""
