resource "google_monitoring_alert_policy" "nibas_backend_alert_policy" {
  display_name = "nibas-backend alert policy"
  combiner     = "OR"
  conditions {
    display_name = "Error condition"
    condition_matched_log {
      filter = "resource.labels.container_name=nibas-backend AND jsonPayload.level=ERROR"

    }
  }
  #notification_channels = [data.google_monitoring_notification_channel.slack_smia_alerts.name]

  alert_strategy {
    notification_rate_limit {
      period = "300s"
    }
  }
}

#resource "google_monitoring_notification_channel" "slack_smia_alerts" {
#  display_name = "Smia slack alerts"
#  type         = "slack"
#  labels = {
#    "channel_name" = "#smia-alerts"
#  }
#  sensitive_labels {
#    auth_token = data.google_secret_manager_secret_version.slack_token.secret_data
#  }
#}

#data "google_secret_manager_secret_version" "slack_token" {
#  secret  = "slack-token"
#  project = var.nibas_project_id
#}
#
#data "google_monitoring_notification_channel" "slack_smia_alerts" {
#  display_name = "Smia slack alerts"
#}


