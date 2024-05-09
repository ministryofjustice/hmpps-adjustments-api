package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

data class OutcomeAndHearing(
  val hearing: Hearing?,
  val outcome: NestedOutcome?,
)
