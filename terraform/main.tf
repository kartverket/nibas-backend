terraform {
  backend "gcs" {}
  required_version = "~> 1.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.6"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "vault" {}

locals {
  environment = var.deploy_env
  namespace   = "nibas"
  app_name    = "nibas-backend"
}
