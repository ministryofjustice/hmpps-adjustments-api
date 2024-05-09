package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

import java.time.LocalDateTime

data class Hearing(
  val dateTimeOfHearing: LocalDateTime,
  val agencyId: String,
)
