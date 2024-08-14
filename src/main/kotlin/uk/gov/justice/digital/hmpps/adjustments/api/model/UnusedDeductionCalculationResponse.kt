package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.model.calculatereleasedatesapi.CrdsValidationMessage

data class UnusedDeductionCalculationResponse(
  val unusedDeductions: Int?,
  val validationMessages: List<CrdsValidationMessage> = emptyList(),
)
