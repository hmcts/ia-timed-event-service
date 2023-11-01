variable "product" {}

variable "raw_product" {
  default = "ia"
}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {
  default = ""
}

variable "common_tags" {
  type = map(string)
}

variable "postgresql_database_name" {
  default = "ia_timed_event_service"
}

variable "postgresql_user" {
  default = "ia_tes"
}

variable "aks_subscription_id" {
}
