package uk.gov.justice.digital.hmpps.adjustments.api.entity

enum class ChangeType {
  CREATE,
  UPDATE,
  DELETE,
  RELEASE,
  ADMISSION,
  MERGE,
  RESET_DAYS,
}
