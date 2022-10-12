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
      replicas = {
        min                  = 2
        max                  = 2
        targetCpuUtilization = 80
      }

      # Environment variables that will be set inside the Deployment's pod
      env = [
        {
          name = "KUBERNETES_CLUSTER"
          value = var.kubernetes_cluster
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
        initialDelay     = 60
      }

      readiness = {
        path = "/actuator/health"
        port = 8080
      }

      resources = {
        limits = {
          cpu    = "1000m"
          memory = "1G"
        }
        requests = {
          cpu    = "500m"
          memory = "500M"
        }
      }

      accessPolicy = {
        outbound = {
          rules = [
            {
              application = "vault"
              namespace   = "vault"
            }
          ]

          external = [
            {
              host = "rin-db1551.statkart.no"
              ip   = "159.162.25.184"
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
