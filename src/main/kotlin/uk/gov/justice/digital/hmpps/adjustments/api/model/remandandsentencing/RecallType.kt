package uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing

data class RecallType(
  val isRecall: Boolean,
  val type: String,
  val isFixedTermRecall: Boolean,
  val lengthInDays: Int,
)
