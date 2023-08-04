package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Validation code details")
enum class ValidationCode(val message: String, val validationType: ValidationType = ValidationType.VALIDATION) {
  RADA_FROM_DATE_NOT_NULL("You must enter a from date for RADA."),
  RADA_REDUCES_BY_MORE_THAN_HALF("Are you sure you want to add more than 50%% of the ADA time for this RADA?", ValidationType.WARNING),
  MORE_RADAS_THAN_ADAS("The RADA time must be less than the ADA time.\nEnter the correct RADA time to continue."),
  RADA_DATE_CANNOT_BE_FUTURE("The RADA date must be in the past, not the future.\nEnter a date in the past to continue."),
  RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE("The date of days restored must be on or after the earliest sentence date, %s."),
  RADA_DAYS_MUST_BE_POSTIVE("The number of days restored must entered."),
  UAL_FROM_DATE_NOT_NULL("You must enter a date for the first day of UAL."),
  UAL_TO_DATE_NOT_NULL("You must enter a date for the last day of UAL."),
  UAL_FROM_DATE_AFTER_TO_DATE("The first day of unlawfully at large date must be before the last day of unlawfully at large date."),
  UAL_TYPE_NOT_NULL("You must select the type of UAL."),
  UAL_FIRST_DATE_CANNOT_BE_FUTURE("The first day of unlawfully at large date must not be in the future"),
  UAL_LAST_DATE_CANNOT_BE_FUTURE("The last day of unlawfully at large date must not be in the future"),
  UAL_DATE_MUST_BE_AFTER_SENTENCE_DATE("The first day of unlawfully at large date must be on or after the start of the sentence, %s"),
}
