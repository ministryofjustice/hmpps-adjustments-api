package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

import java.time.LocalDate

data class Schedule(
  val duration: Int,
  val suspendedUntil: LocalDate?,
)
