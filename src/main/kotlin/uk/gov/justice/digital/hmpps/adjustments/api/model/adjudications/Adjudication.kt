package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

data class Adjudication(
  val chargeNumber: String,
  val prisonerNumber: String,
  val status: String,
  val outcomes: List<AdjudicationOutcome>,
  val punishments: List<Punishment>,
)
