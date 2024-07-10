package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

const val RECALL_COURT_EVENT = "1501"
data class CourtDateChargeAndOutcomes(
  val chargeId: Long? = null,
  val outcomes: List<CourtDateOutcome>,
)
