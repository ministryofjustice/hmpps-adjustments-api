package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDate

data class CourtDateOutcome(

  val id: Long,

  val date: LocalDate? = null,

  val resultCode: String? = null,

  val resultDescription: String? = null,

  val resultDispositionCode: String? = null,

)
