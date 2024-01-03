package uk.gov.justice.digital.hmpps.adjustments.api.entity

enum class AdjustmentType {
  REMAND,
  TAGGED_BAIL,
  UNLAWFULLY_AT_LARGE,
  LAWFULLY_AT_LARGE,
  ADDITIONAL_DAYS_AWARDED,
  RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
  SPECIAL_REMISSION,
  UNUSED_DEDUCTIONS,
  ;


  fun isSentenceType(): Boolean {
    return this == REMAND || this == TAGGED_BAIL || this == UNUSED_DEDUCTIONS
  }
}
