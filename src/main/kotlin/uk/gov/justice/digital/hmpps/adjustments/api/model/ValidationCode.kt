package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Validation code details")
enum class ValidationCode(val message: String, val validationType: ValidationType = ValidationType.VALIDATION) {
  RADA_REDUCES_BY_MORE_THAN_HALF("Are you sure, as this reduction is more than 50%% of the total additional days awarded?", ValidationType.WARNING),
  MORE_RADAS_THAN_ADAS("The number of days restored cannot be more than the number of days rewarded."),
  RADA_DATE_CANNOT_BE_FUTURE("Enter a Date of days restored date which is on or before today's date."),
  RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE("The date of days restored must be on or after start of sentences, %s."),
}
