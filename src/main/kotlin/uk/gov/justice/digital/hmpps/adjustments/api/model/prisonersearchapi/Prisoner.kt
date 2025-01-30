package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonersearchapi

import java.time.LocalDate

data class Prisoner(
  val prisonerNumber: String,
  val bookingId: Long,
  val prisonId: String = "",
  val firstName: String = "",
  val lastName: String = "",
  val dateOfBirth: LocalDate,
)
