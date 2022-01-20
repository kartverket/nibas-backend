terraform {
  backend "gcs" {
    bucket  = "terraform-state-nibas-backend"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

locals {
  namespace = "nibas"
}

variable "nibas_backend_version" {}

resource "kubernetes_deployment" "nibas-backend-deployment" {
  metadata {
    name      = "nibas-backend"
    namespace = local.namespace
    labels = {
      "backstage.io/kubernetes-id" = "nibas"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "nibas-backend"
      }
    }
    template {
      metadata {
        annotations = {
          "prometheus.io/scrape" = "true"
        }
        labels = {
          app = "nibas-backend"
          "backstage.io/kubernetes-id" = "nibas"
        }
      }
      spec {
        image_pull_secrets {
          name="nibas-db-pull-token"
        }
        container {
          image = "ghcr.io/kartverket/nibas-backend:${var.nibas_backend_version}"
          name  = "nibas-backend"
          resources {
            requests = {
              memory = "3Gi"
            }
            limits = {
              memory = "3Gi"
            }
          }
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            #TODO: Hente dette fra "et sted"
            value = "test"
          }
          port {
            container_port = 8080
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "nibas-backend-service" {
  metadata {
    name      = "nibas-backend-service"
    namespace = local.namespace
#    annotations = {
#      "external-dns.alpha.kubernetes.io/hostname" = "nibas-backend.tz2-test-apps.k8s.local"
#    }
  }
  spec {
    selector = {
      app = "nibas-backend"
    }
    port {
      protocol    = "TCP"
      port        = 80
      target_port = 8080
    }
    type     = "NodePort"
  }
}
