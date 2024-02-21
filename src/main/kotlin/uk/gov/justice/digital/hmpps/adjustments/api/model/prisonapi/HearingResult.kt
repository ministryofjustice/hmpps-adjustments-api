package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

data class HearingResult (
  val offenceType: String? = null,
  val offenceDescription: String? = null,
  val plea: String? = null,
  val finding: String? = null,
  val sanctions: List<Sanction>? = null,
)
