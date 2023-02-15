terraform {
  backend "gcs" {}
  required_version = "~> 1.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.6"
    }

    google = {
      source  = "hashicorp/google"
      version = "4.53.1"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "vault" {
  address         = var.vault_addr
  skip_tls_verify = true
}

provider "google" {}

locals {
  environment = var.deploy_env
  namespace   = "nibas"
  app_name    = "nibas-backend"
}
