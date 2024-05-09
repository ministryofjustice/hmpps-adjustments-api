package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

data class Punishment(
  val type: String,
  val schedule: Schedule,
  val consecutiveChargeNumber: String?,
)
