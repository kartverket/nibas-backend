data "vault_generic_secret" "nibas_db_details" {
  path = "nibas/nibas-db"
}

resource "kubernetes_manifest" "nibas_backend_application" {
  manifest = {
    apiVersion = "skiperator.kartverket.no/v1alpha1"
    kind       = "Application"
    metadata = {
      name      = "nibas-backend"
      namespace = "nibas"
    }
    spec = {
      image = var.image
      port  = 8080
      # Any external hostnames that route to this application
      ingresses = [var.external_dns_hostname]
      gcp = {
        auth = {
          serviceAccount = var.sa_pub_sub_publisher
        }
      }
      replicas = {
        min                  = 2
        max                  = 2
        targetCpuUtilization = 80
      }

      # Environment variables that will be set inside the Deployment's pod
      env = [
        {
          name  = "KUBERNETES_CLUSTER"
          value = var.kubernetes_cluster
        },
        {
          name  = "GOOGLE_CLOUD_PROJECT"
          value = var.nibas_project_id
        }
      ]
      strategy = {
        type = "RollingUpdate"
      }

      # Liveness probes define a resource that returns 200 OK when the app is running
      # as intended. Returning a non-200 code will make kubernetes restart the app.
      # Liveness is optional, but when provided path and port is required
      liveness = {
        path             = "/actuator/health"
        port             = 8080
        failureThreshold = 3
        timeout          = 1
        initialDelay     = 75
      }

      readiness = {
        path = "/actuator/health"
        port = 8080
      }

      resources = {
        limits = {
          memory = "1G"
        }
        requests = {
          cpu    = "10m"
          memory = "500M"
        }
      }

      accessPolicy = {
        inbound = {
          rules = [
            {
              application = "nibas-frontend"
            }
          ]
        }
        outbound = {
          rules = [
            {
              application = "vault"
              namespace   = "vault"
            },
            {
              application = "aut-idporten"
              namespace   = "aut"
            },
            {
              application = "nibas-db" # Kun til vi kutter kubernetes-base
            }
          ]

          external = [
            {
              host = data.vault_generic_secret.nibas_db_details.data["db-host"]
              ip   = data.vault_generic_secret.nibas_db_details.data["db-ip"]
              ports = [
                {
                  name     = "postgisport"
                  protocol = "TCP"
                  port : 5432
                }
              ]
            },
            {
              host = "register.geonorge.no"
            }
          ]
        }

      }
    }
  }
}
