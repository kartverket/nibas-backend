resource "random_password" "matrikkel_api_key" {
  length  = 29
  special = true
  lower   = true
  upper   = true
  numeric = true
}

resource "vault_generic_secret" "matrikkel_api_key_vault" {
  path = "nibas/nibas-backend-apikeys"

  data_json = <<EOT
{
  "api.key.matrikkel":   "${random_password.matrikkel_api_key}"
}
EOT
}
