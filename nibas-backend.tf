terraform {
  backend "gcs" {
    prefix = "nibas-backend"
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
          "seccomp.security.alpha.kubernetes.io/pod" = "runtime/default"
        }
        labels = {
          app = "nibas-backend"
          "backstage.io/kubernetes-id" = "nibas"
        }
      }
      spec {
        image_pull_secrets {
          # Midlertidig PAT for Kenneth
          name="nibas-pull-token-atkv1-kes"
        }
        security_context {
          supplemental_groups = [199]
          fs_group = 199
        }
        container {
          image = "ghcr.io/kartverket/nibas-backend:${var.nibas_backend_version}"
          name  = "nibas-backend"
          security_context {
            privileged                 = false # Normal priviliges
            allow_privilege_escalation = false # Prevent reqests for root priviliges
            read_only_root_filesystem  = true  # Prevent writing to system files
            run_as_user                = 199   # Run as an unpriviliged user
            run_as_group               = 199   # Run as an unpriviliged group
          }
          resources {
            requests = {
              memory = "1Gi"
            }
            limits = {
              memory = "1Gi"
            }
          }
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            #TODO: Hente dette fra "et sted"
            value = "test,security-off"
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
    name      = "nibas-backend"
    namespace = local.namespace
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
    type     = "ClusterIP"
  }
}

resource "kubernetes_manifest" "istio-destination-rule" {
  manifest = yamldecode(file("${path.module}/kubernetes/destination-rule.yaml"))
}

resource "kubernetes_manifest" "istio-gateway" {
  manifest = yamldecode(file("${path.module}/kubernetes/gateway.yaml"))
}

resource "kubernetes_manifest" "istio-virtualservice" {
  manifest = yamldecode(file("${path.module}/kubernetes/virtualservice.yaml"))
}
