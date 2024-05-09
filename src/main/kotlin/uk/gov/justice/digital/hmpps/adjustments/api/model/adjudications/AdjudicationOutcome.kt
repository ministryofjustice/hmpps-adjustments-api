package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

data class AdjudicationOutcome(
  val hearing: Hearing?,
  val outcome: Outcome?,
)
