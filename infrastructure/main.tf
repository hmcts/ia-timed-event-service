provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "cft_vnet"
  subscription_id            = var.aks_subscription_id
}

locals {

  preview_vault_name           = "${var.raw_product}-aat"
  non_preview_vault_name       = "${var.raw_product}-${var.env}"
  key_vault_name               = var.env == "preview" || var.env == "spreview" ? local.preview_vault_name : local.non_preview_vault_name

}

data "azurerm_key_vault" "ia_key_vault" {
  name                = local.key_vault_name
  resource_group_name = local.key_vault_name
}

module "ia_timed_event_service_database_11" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}-${var.component}-postgres-11-db"
  location           = "${var.location}"
  env                = "${var.env}"
  database_name      = "${var.postgresql_database_name}"
  postgresql_user    = "${var.postgresql_user}"
  postgresql_version = "11"
  common_tags        = "${merge(var.common_tags, tomap({"lastUpdated" = "${timestamp()}"}))}"
  subscription       = "${var.subscription}"
  backup_retention_days = "${var.database_backup_retention_days}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-11" {
  name         = "${var.component}-POSTGRES-PASS-11"
  value        = "${module.ia_timed_event_service_database_11.postgresql_password}"
  key_vault_id = "${data.azurerm_key_vault.ia_key_vault.id}"
}

