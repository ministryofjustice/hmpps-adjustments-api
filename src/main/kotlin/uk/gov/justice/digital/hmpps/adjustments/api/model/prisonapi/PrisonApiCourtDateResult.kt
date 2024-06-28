package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
const val RECALL_COURT_EVENT = "1501"

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonApiCourtDateResult(
  val id: Long = -1,
  val date: LocalDate = LocalDate.now(),
  val resultCode: String? = null,
  val resultDescription: String? = null,
  val resultDispositionCode: String? = null,
  val charge: PrisonApiCharge = PrisonApiCharge(),
  val bookingId: Long = -1,
  val bookNumber: String = "",
)
