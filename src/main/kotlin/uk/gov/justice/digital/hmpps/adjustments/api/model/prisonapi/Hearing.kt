package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDateTime

data class Hearing (
  val hearingTime: LocalDateTime,
  val results: List<HearingResult>? = null,
  val establishment: String? = null
)
