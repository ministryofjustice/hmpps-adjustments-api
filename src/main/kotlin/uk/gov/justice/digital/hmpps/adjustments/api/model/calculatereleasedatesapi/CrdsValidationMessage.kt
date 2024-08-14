package uk.gov.justice.digital.hmpps.adjustments.api.model.calculatereleasedatesapi

data class CrdsValidationMessage(
  val code: String,
  val message: String,
  val type: String,
  val arguments: List<String> = listOf(),
)
