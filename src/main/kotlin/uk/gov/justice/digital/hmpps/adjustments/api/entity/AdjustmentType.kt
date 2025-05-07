package uk.gov.justice.digital.hmpps.adjustments.api.entity

import uk.gov.justice.digital.hmpps.adjustments.api.enums.ArithmeticType

enum class AdjustmentType(val text: String, val arithmeticType: ArithmeticType) {
  REMAND("Remand", ArithmeticType.DEDUCTION),
  TAGGED_BAIL("Tagged bail", ArithmeticType.DEDUCTION),
  UNLAWFULLY_AT_LARGE("UAL (Unlawfully at large)", ArithmeticType.ADDITION),
  LAWFULLY_AT_LARGE("Lawfully at large", ArithmeticType.NONE),
  ADDITIONAL_DAYS_AWARDED("ADA (Additional days awarded)", ArithmeticType.ADDITION),
  RESTORATION_OF_ADDITIONAL_DAYS_AWARDED("RADA (Restoration of additional days awarded)", ArithmeticType.DEDUCTION),
  SPECIAL_REMISSION("Special remission", ArithmeticType.NONE),
  UNUSED_DEDUCTIONS("Unused deductions", ArithmeticType.NONE),
  CUSTODY_ABROAD("Time spent in custody abroad", ArithmeticType.DEDUCTION),
  APPEAL_APPLICANT("Time spent as an appeal applicant", ArithmeticType.ADDITION),
  ;

  fun isSentenceType(): Boolean = this == REMAND || this == TAGGED_BAIL || this == UNUSED_DEDUCTIONS || this == CUSTODY_ABROAD || this == APPEAL_APPLICANT
}
