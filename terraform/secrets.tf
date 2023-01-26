resource "google_secret_manager_secret" "slack-token" {
  secret_id = "slack-token"
  project   = var.nibas_project_id

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_iam_member" "secret-writer" {
  project   = var.nibas_project_id
  secret_id = google_secret_manager_secret.slack-token.secret_id
  role      = "roles/secretmanager.secretVersionAdder"
  member    = "user:sofie.urhaug@kartverket.no"
}
