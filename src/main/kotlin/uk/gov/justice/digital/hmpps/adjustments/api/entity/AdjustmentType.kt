package uk.gov.justice.digital.hmpps.adjustments.api.entity

enum class AdjustmentType(val text: String) {
  REMAND("Remand"),
  TAGGED_BAIL("Tagged bail"),
  UNLAWFULLY_AT_LARGE("Unlawfully at large (UAL)"),
  LAWFULLY_AT_LARGE("Lawfully at large"),
  ADDITIONAL_DAYS_AWARDED("Additional days awarded (ADA)"),
  RESTORATION_OF_ADDITIONAL_DAYS_AWARDED("RADA (Restoration of added days)"),
  SPECIAL_REMISSION("Special remission"),
  UNUSED_DEDUCTIONS("Unused deductions"),
  ;

  fun isSentenceType(): Boolean {
    return this == REMAND || this == TAGGED_BAIL || this == UNUSED_DEDUCTIONS
  }
}
