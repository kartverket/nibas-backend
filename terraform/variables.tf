variable "image" {
  type = string
}

variable "external_dns_hostname" {
  type = string
}

variable "container_registry" {
  type = string
}

variable "container_registry_username" {
  type = string
}

variable "deploy_env" {
  type = string
}

variable "kubernetes_cluster" {
  type = string
}

variable "vault_addr" {
  type = string
}
