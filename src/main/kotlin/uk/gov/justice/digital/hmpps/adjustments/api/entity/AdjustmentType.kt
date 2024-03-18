package uk.gov.justice.digital.hmpps.adjustments.api.entity

enum class AdjustmentType(val text: String) {
  REMAND("Remand"),
  TAGGED_BAIL("Tagged bail"),
  UNLAWFULLY_AT_LARGE("UAL (Unlawfully at large)"),
  LAWFULLY_AT_LARGE("Lawfully at large"),
  ADDITIONAL_DAYS_AWARDED("ADA (Additional days awarded)"),
  RESTORATION_OF_ADDITIONAL_DAYS_AWARDED("RADA (Restoration of additional days awarded)"),
  SPECIAL_REMISSION("Special remission"),
  UNUSED_DEDUCTIONS("Unused deductions"),
  ;

  fun isSentenceType(): Boolean {
    return this == REMAND || this == TAGGED_BAIL || this == UNUSED_DEDUCTIONS
  }
}
