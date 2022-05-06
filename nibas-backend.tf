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
