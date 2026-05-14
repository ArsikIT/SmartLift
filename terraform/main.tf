locals {
  common_labels = {
    app         = "smartlift"
    managed-by  = "terraform"
    environment = var.environment
  }

  backend_labels = merge(local.common_labels, {
    component = "backend"
  })

  postgres_labels = merge(local.common_labels, {
    component = "postgres"
  })

  frontend_labels = merge(local.common_labels, {
    component = "frontend"
  })

  prometheus_labels = merge(local.common_labels, {
    component = "prometheus"
  })

  grafana_labels = merge(local.common_labels, {
    component = "grafana"
  })

  alertmanager_labels = merge(local.common_labels, {
    component = "alertmanager"
  })

  node_exporter_labels = merge(local.common_labels, {
    component = "node-exporter"
  })

  postgres_service_name = "smartlift-postgres"
  db_url_effective      = var.db_url != "" ? var.db_url : "jdbc:postgresql://${local.postgres_service_name}.${var.app_namespace}.svc.cluster.local:5432/${var.postgres_db}"

  frontend_nginx_config = <<-EOT
    server {
        listen 80;
        server_name _;

        root /usr/share/nginx/html;
        index index.html;

        location / {
            try_files $uri $uri/ /index.html;
        }

        location /api/ {
            proxy_pass ${var.frontend_api_upstream};
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
  EOT

  prometheus_config = <<-EOT
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    alerting:
      alertmanagers:
        - static_configs:
            - targets: ["smartlift-alertmanager:9093"]

    rule_files:
      - /etc/prometheus/alert_rules.yml

    scrape_configs:
      - job_name: prometheus
        static_configs:
          - targets: ["smartlift-prometheus:9090"]

      - job_name: smartlift-backend
        metrics_path: /actuator/prometheus
        static_configs:
          - targets: ["smartlift-backend.${var.app_namespace}.svc.cluster.local:8080"]

      - job_name: node-exporter
        static_configs:
          - targets: ["smartlift-node-exporter:9100"]
  EOT

  alert_rules                = file("${path.module}/../observability/prometheus/alert_rules.yml")
  alertmanager_config        = file("${path.module}/../observability/alertmanager/alertmanager.yml")
  grafana_datasource_config  = file("${path.module}/../observability/grafana/provisioning/datasources/datasource.yml")
  grafana_dashboard_provider = file("${path.module}/../observability/grafana/provisioning/dashboards/dashboard-provider.yml")
  grafana_dashboard_json     = file("${path.module}/../observability/grafana/dashboards/smartlift-overview.json")
}

resource "kubernetes_namespace_v1" "app" {
  metadata {
    name = var.app_namespace

    labels = {
      name = var.app_namespace
    }
  }
}

resource "kubernetes_namespace_v1" "observability" {
  metadata {
    name = var.observability_namespace

    labels = {
      name = var.observability_namespace
    }
  }
}

resource "kubernetes_config_map_v1" "backend" {
  metadata {
    name      = "smartlift-backend-config"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.backend_labels
  }

  data = {
    SPRING_PROFILES_ACTIVE = "prod"
    DB_URL                 = local.db_url_effective
    DB_USERNAME            = var.db_username
    JWT_EXPIRATION         = var.jwt_expiration
    MAIL_ENABLED           = tostring(var.mail_enabled)
    MAIL_FROM              = var.mail_from
  }
}

resource "kubernetes_secret_v1" "backend" {
  metadata {
    name      = "smartlift-backend-secrets"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.backend_labels
  }

  type = "Opaque"

  data = {
    DB_PASSWORD   = var.db_password
    JWT_SECRET    = var.jwt_secret
    MAIL_HOST     = var.mail_host
    MAIL_PORT     = var.mail_port
    MAIL_USERNAME = var.mail_username
    MAIL_PASSWORD = var.mail_password
  }
}

resource "kubernetes_secret_v1" "postgres" {
  metadata {
    name      = "smartlift-postgres-secrets"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.postgres_labels
  }

  type = "Opaque"

  data = {
    POSTGRES_DB       = var.postgres_db
    POSTGRES_USER     = var.db_username
    POSTGRES_PASSWORD = var.db_password
  }
}

resource "kubernetes_service_v1" "postgres" {
  metadata {
    name      = local.postgres_service_name
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.postgres_labels
  }

  spec {
    selector = local.postgres_labels

    port {
      name        = "postgresql"
      port        = 5432
      target_port = 5432
    }

    cluster_ip = "None"
  }
}

resource "kubernetes_stateful_set_v1" "postgres" {
  metadata {
    name      = local.postgres_service_name
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.postgres_labels
  }

  spec {
    service_name = kubernetes_service_v1.postgres.metadata[0].name
    replicas     = 1

    selector {
      match_labels = local.postgres_labels
    }

    template {
      metadata {
        labels = local.postgres_labels
      }

      spec {
        container {
          name              = "postgres"
          image             = var.postgres_image
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 5432
            name           = "postgresql"
          }

          env {
            name = "POSTGRES_DB"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.postgres.metadata[0].name
                key  = "POSTGRES_DB"
              }
            }
          }

          env {
            name = "POSTGRES_USER"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.postgres.metadata[0].name
                key  = "POSTGRES_USER"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.postgres.metadata[0].name
                key  = "POSTGRES_PASSWORD"
              }
            }
          }

          resources {
            requests = {
              cpu    = var.postgres_cpu_request
              memory = var.postgres_memory_request
            }

            limits = {
              cpu    = var.postgres_cpu_limit
              memory = var.postgres_memory_limit
            }
          }

          readiness_probe {
            tcp_socket {
              port = 5432
            }

            initial_delay_seconds = 10
            period_seconds        = 10
            timeout_seconds       = 3
            failure_threshold     = 6
          }

          liveness_probe {
            tcp_socket {
              port = 5432
            }

            initial_delay_seconds = 30
            period_seconds        = 15
            timeout_seconds       = 3
            failure_threshold     = 6
          }

          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql/data"
          }
        }
      }
    }

    volume_claim_template {
      metadata {
        name = "postgres-data"
      }

      spec {
        access_modes = ["ReadWriteOnce"]

        resources {
          requests = {
            storage = var.postgres_storage_size
          }
        }

        storage_class_name = var.postgres_storage_class_name != "" ? var.postgres_storage_class_name : null
      }
    }
  }
}

resource "kubernetes_deployment_v1" "backend" {
  metadata {
    name      = "smartlift-backend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.backend_labels
  }

  spec {
    replicas               = var.backend_replicas
    revision_history_limit = 2

    selector {
      match_labels = local.backend_labels
    }

    template {
      metadata {
        labels = local.backend_labels
      }

      spec {
        container {
          name              = "backend"
          image             = var.backend_image
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8080
            name           = "http"
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = kubernetes_config_map_v1.backend.data["SPRING_PROFILES_ACTIVE"]
          }

          env {
            name  = "DB_URL"
            value = kubernetes_config_map_v1.backend.data["DB_URL"]
          }

          env {
            name  = "DB_USERNAME"
            value = kubernetes_config_map_v1.backend.data["DB_USERNAME"]
          }

          env {
            name = "DB_PASSWORD"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "DB_PASSWORD"
              }
            }
          }

          env {
            name = "JWT_SECRET"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "JWT_SECRET"
              }
            }
          }

          env {
            name  = "JWT_EXPIRATION"
            value = kubernetes_config_map_v1.backend.data["JWT_EXPIRATION"]
          }

          env {
            name  = "MAIL_ENABLED"
            value = kubernetes_config_map_v1.backend.data["MAIL_ENABLED"]
          }

          env {
            name  = "MAIL_FROM"
            value = kubernetes_config_map_v1.backend.data["MAIL_FROM"]
          }

          env {
            name = "MAIL_HOST"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "MAIL_HOST"
              }
            }
          }

          env {
            name = "MAIL_PORT"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "MAIL_PORT"
              }
            }
          }

          env {
            name = "MAIL_USERNAME"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "MAIL_USERNAME"
              }
            }
          }

          env {
            name = "MAIL_PASSWORD"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.backend.metadata[0].name
                key  = "MAIL_PASSWORD"
              }
            }
          }

          resources {
            requests = {
              cpu    = var.backend_cpu_request
              memory = var.backend_memory_request
            }

            limits = {
              cpu    = var.backend_cpu_limit
              memory = var.backend_memory_limit
            }
          }

          startup_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = "http"
            }

            failure_threshold = 30
            period_seconds    = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = "http"
            }

            initial_delay_seconds = 10
            period_seconds        = 10
            timeout_seconds       = 3
            failure_threshold     = 3
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = "http"
            }

            initial_delay_seconds = 30
            period_seconds        = 15
            timeout_seconds       = 3
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "backend" {
  metadata {
    name      = "smartlift-backend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.backend_labels
  }

  spec {
    selector = local.backend_labels

    port {
      name        = "http"
      port        = 8080
      target_port = "http"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_horizontal_pod_autoscaler_v2" "backend" {
  metadata {
    name      = "smartlift-backend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.backend_labels
  }

  spec {
    min_replicas = var.backend_min_replicas
    max_replicas = var.backend_max_replicas

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment_v1.backend.metadata[0].name
    }

    behavior {
      scale_up {
        stabilization_window_seconds = 0
        select_policy                = "Max"

        policy {
          type           = "Percent"
          value          = 100
          period_seconds = 60
        }

        policy {
          type           = "Pods"
          value          = 2
          period_seconds = 60
        }
      }

      scale_down {
        stabilization_window_seconds = 300
        select_policy                = "Max"

        policy {
          type           = "Percent"
          value          = 50
          period_seconds = 60
        }
      }
    }

    metric {
      type = "Resource"

      resource {
        name = "cpu"

        target {
          type                = "Utilization"
          average_utilization = var.backend_hpa_target_cpu_utilization
        }
      }
    }
  }
}

resource "kubernetes_config_map_v1" "frontend_nginx" {
  metadata {
    name      = "smartlift-frontend-nginx"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.frontend_labels
  }

  data = {
    "default.conf" = local.frontend_nginx_config
  }
}

resource "kubernetes_deployment_v1" "frontend" {
  metadata {
    name      = "smartlift-frontend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.frontend_labels
  }

  spec {
    replicas               = var.frontend_replicas
    revision_history_limit = 2

    selector {
      match_labels = local.frontend_labels
    }

    template {
      metadata {
        labels = local.frontend_labels
      }

      spec {
        container {
          name              = "frontend"
          image             = var.frontend_image
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 80
            name           = "http"
          }

          resources {
            requests = {
              cpu    = var.frontend_cpu_request
              memory = var.frontend_memory_request
            }

            limits = {
              cpu    = var.frontend_cpu_limit
              memory = var.frontend_memory_limit
            }
          }

          readiness_probe {
            http_get {
              path = "/"
              port = "http"
            }

            initial_delay_seconds = 5
            period_seconds        = 10
            timeout_seconds       = 3
            failure_threshold     = 3
          }

          liveness_probe {
            http_get {
              path = "/"
              port = "http"
            }

            initial_delay_seconds = 15
            period_seconds        = 15
            timeout_seconds       = 3
            failure_threshold     = 3
          }

          volume_mount {
            name       = "nginx-config"
            mount_path = "/etc/nginx/conf.d/default.conf"
            sub_path   = "default.conf"
          }
        }

        volume {
          name = "nginx-config"

          config_map {
            name = kubernetes_config_map_v1.frontend_nginx.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "frontend" {
  metadata {
    name      = "smartlift-frontend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.frontend_labels
  }

  spec {
    selector = local.frontend_labels

    port {
      name        = "http"
      port        = 80
      target_port = "http"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_ingress_v1" "frontend" {
  count = var.ingress_enabled ? 1 : 0

  metadata {
    name      = "smartlift-frontend"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    labels    = local.frontend_labels
  }

  spec {
    ingress_class_name = var.ingress_class_name

    rule {
      host = var.frontend_ingress_host

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = kubernetes_service_v1.frontend.metadata[0].name

              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_config_map_v1" "prometheus" {
  metadata {
    name      = "smartlift-prometheus-config"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.prometheus_labels
  }

  data = {
    "prometheus.yml"  = local.prometheus_config
    "alert_rules.yml" = local.alert_rules
  }
}

resource "kubernetes_deployment_v1" "prometheus" {
  metadata {
    name      = "smartlift-prometheus"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.prometheus_labels
  }

  spec {
    replicas = 1

    selector {
      match_labels = local.prometheus_labels
    }

    template {
      metadata {
        labels = local.prometheus_labels
      }

      spec {
        container {
          name              = "prometheus"
          image             = var.prometheus_image
          image_pull_policy = "IfNotPresent"

          args = [
            "--config.file=/etc/prometheus/prometheus.yml"
          ]

          port {
            container_port = 9090
            name           = "http"
          }

          resources {
            requests = {
              cpu    = var.prometheus_cpu_request
              memory = var.prometheus_memory_request
            }

            limits = {
              cpu    = var.prometheus_cpu_limit
              memory = var.prometheus_memory_limit
            }
          }

          volume_mount {
            name       = "prometheus-config"
            mount_path = "/etc/prometheus/prometheus.yml"
            sub_path   = "prometheus.yml"
          }

          volume_mount {
            name       = "prometheus-config"
            mount_path = "/etc/prometheus/alert_rules.yml"
            sub_path   = "alert_rules.yml"
          }

          volume_mount {
            name       = "prometheus-data"
            mount_path = "/prometheus"
          }
        }

        volume {
          name = "prometheus-config"

          config_map {
            name = kubernetes_config_map_v1.prometheus.metadata[0].name
          }
        }

        volume {
          name = "prometheus-data"

          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "prometheus" {
  metadata {
    name      = "smartlift-prometheus"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.prometheus_labels
  }

  spec {
    selector = local.prometheus_labels

    port {
      name        = "http"
      port        = 9090
      target_port = "http"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_config_map_v1" "alertmanager" {
  metadata {
    name      = "smartlift-alertmanager-config"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.alertmanager_labels
  }

  data = {
    "alertmanager.yml" = local.alertmanager_config
  }
}

resource "kubernetes_deployment_v1" "alertmanager" {
  metadata {
    name      = "smartlift-alertmanager"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.alertmanager_labels
  }

  spec {
    replicas = 1

    selector {
      match_labels = local.alertmanager_labels
    }

    template {
      metadata {
        labels = local.alertmanager_labels
      }

      spec {
        container {
          name              = "alertmanager"
          image             = var.alertmanager_image
          image_pull_policy = "IfNotPresent"

          args = [
            "--config.file=/etc/alertmanager/alertmanager.yml",
            "--storage.path=/alertmanager"
          ]

          port {
            container_port = 9093
            name           = "http"
          }

          resources {
            requests = {
              cpu    = var.alertmanager_cpu_request
              memory = var.alertmanager_memory_request
            }

            limits = {
              cpu    = var.alertmanager_cpu_limit
              memory = var.alertmanager_memory_limit
            }
          }

          volume_mount {
            name       = "alertmanager-config"
            mount_path = "/etc/alertmanager/alertmanager.yml"
            sub_path   = "alertmanager.yml"
          }

          volume_mount {
            name       = "alertmanager-data"
            mount_path = "/alertmanager"
          }
        }

        volume {
          name = "alertmanager-config"

          config_map {
            name = kubernetes_config_map_v1.alertmanager.metadata[0].name
          }
        }

        volume {
          name = "alertmanager-data"

          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "alertmanager" {
  metadata {
    name      = "smartlift-alertmanager"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.alertmanager_labels
  }

  spec {
    selector = local.alertmanager_labels

    port {
      name        = "http"
      port        = 9093
      target_port = "http"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_secret_v1" "grafana" {
  metadata {
    name      = "smartlift-grafana-secrets"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  type = "Opaque"

  data = {
    GF_SECURITY_ADMIN_USER     = var.grafana_admin_user
    GF_SECURITY_ADMIN_PASSWORD = var.grafana_admin_password
  }
}

resource "kubernetes_config_map_v1" "grafana_datasources" {
  metadata {
    name      = "smartlift-grafana-datasources"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  data = {
    "datasource.yml" = local.grafana_datasource_config
  }
}

resource "kubernetes_config_map_v1" "grafana_dashboard_provider" {
  metadata {
    name      = "smartlift-grafana-dashboard-provider"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  data = {
    "dashboard-provider.yml" = local.grafana_dashboard_provider
  }
}

resource "kubernetes_config_map_v1" "grafana_dashboard" {
  metadata {
    name      = "smartlift-grafana-dashboard"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  data = {
    "smartlift-overview.json" = local.grafana_dashboard_json
  }
}

resource "kubernetes_deployment_v1" "grafana" {
  metadata {
    name      = "smartlift-grafana"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  spec {
    replicas = 1

    selector {
      match_labels = local.grafana_labels
    }

    template {
      metadata {
        labels = local.grafana_labels
      }

      spec {
        container {
          name              = "grafana"
          image             = var.grafana_image
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 3000
            name           = "http"
          }

          env {
            name = "GF_SECURITY_ADMIN_USER"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.grafana.metadata[0].name
                key  = "GF_SECURITY_ADMIN_USER"
              }
            }
          }

          env {
            name = "GF_SECURITY_ADMIN_PASSWORD"

            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.grafana.metadata[0].name
                key  = "GF_SECURITY_ADMIN_PASSWORD"
              }
            }
          }

          resources {
            requests = {
              cpu    = var.grafana_cpu_request
              memory = var.grafana_memory_request
            }

            limits = {
              cpu    = var.grafana_cpu_limit
              memory = var.grafana_memory_limit
            }
          }

          volume_mount {
            name       = "grafana-datasources"
            mount_path = "/etc/grafana/provisioning/datasources/datasource.yml"
            sub_path   = "datasource.yml"
          }

          volume_mount {
            name       = "grafana-dashboard-provider"
            mount_path = "/etc/grafana/provisioning/dashboards/dashboard-provider.yml"
            sub_path   = "dashboard-provider.yml"
          }

          volume_mount {
            name       = "grafana-dashboard"
            mount_path = "/etc/grafana/dashboards/smartlift-overview.json"
            sub_path   = "smartlift-overview.json"
          }

          volume_mount {
            name       = "grafana-data"
            mount_path = "/var/lib/grafana"
          }
        }

        volume {
          name = "grafana-datasources"

          config_map {
            name = kubernetes_config_map_v1.grafana_datasources.metadata[0].name
          }
        }

        volume {
          name = "grafana-dashboard-provider"

          config_map {
            name = kubernetes_config_map_v1.grafana_dashboard_provider.metadata[0].name
          }
        }

        volume {
          name = "grafana-dashboard"

          config_map {
            name = kubernetes_config_map_v1.grafana_dashboard.metadata[0].name
          }
        }

        volume {
          name = "grafana-data"

          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "grafana" {
  metadata {
    name      = "smartlift-grafana"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.grafana_labels
  }

  spec {
    selector = local.grafana_labels

    port {
      name        = "http"
      port        = 3000
      target_port = "http"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_deployment_v1" "node_exporter" {
  metadata {
    name      = "smartlift-node-exporter"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.node_exporter_labels
  }

  spec {
    replicas = 1

    selector {
      match_labels = local.node_exporter_labels
    }

    template {
      metadata {
        labels = local.node_exporter_labels
      }

      spec {
        container {
          name              = "node-exporter"
          image             = var.node_exporter_image
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 9100
            name           = "http"
          }

          resources {
            requests = {
              cpu    = var.node_exporter_cpu_request
              memory = var.node_exporter_memory_request
            }

            limits = {
              cpu    = var.node_exporter_cpu_limit
              memory = var.node_exporter_memory_limit
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "node_exporter" {
  metadata {
    name      = "smartlift-node-exporter"
    namespace = kubernetes_namespace_v1.observability.metadata[0].name
    labels    = local.node_exporter_labels
  }

  spec {
    selector = local.node_exporter_labels

    port {
      name        = "http"
      port        = 9100
      target_port = "http"
    }

    type = "ClusterIP"
  }
}
