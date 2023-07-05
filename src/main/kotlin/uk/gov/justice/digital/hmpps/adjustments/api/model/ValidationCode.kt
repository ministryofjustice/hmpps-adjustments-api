package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Validation code details")
enum class ValidationCode(val message: String, val validationType: ValidationType = ValidationType.VALIDATION) {
  RADA_REDUCES_BY_MORE_THAN_HALF("Are you sure you want to add more than 50%% of the ADA time for this RADA?", ValidationType.WARNING),
  MORE_RADAS_THAN_ADAS("The RADA time must be less than the ADA time.\nThe RADA time must be less than the ADA time."),
  RADA_DATE_CANNOT_BE_FUTURE("The RADA date must be in the past, not the future.\nEnter a date in the past to continue."),
  RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE("The date of days restored must be on or after the earliest sentence date, %s."),
}
