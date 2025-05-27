package uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing

data class SentenceDetailResponse(
  val nomisSentenceTypeReference: String,
  val recall: RecallType?,
  val nomisDescription: String,
  val isIndeterminate: Boolean,
  val nomisActive: Boolean,
  val nomisExpiryDate: String?,
)
