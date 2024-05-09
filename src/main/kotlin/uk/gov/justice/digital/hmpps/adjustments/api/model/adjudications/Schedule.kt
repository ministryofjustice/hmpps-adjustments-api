package uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications

import java.time.LocalDate

data class Schedule(
  val days: Int,
  val suspendedUntil: LocalDate?,
)
