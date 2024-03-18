package uk.gov.justice.digital.hmpps.adjustments.api.entity

enum class AdjustmentStatus {
  ACTIVE,
  INACTIVE,
  DELETED,
  INACTIVE_WHEN_DELETED,
  ;

  fun isDeleted(): Boolean {
    return listOf(DELETED, INACTIVE_WHEN_DELETED).contains(this)
  }
}
