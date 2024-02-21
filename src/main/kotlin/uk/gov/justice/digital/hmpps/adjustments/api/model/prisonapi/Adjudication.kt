package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDateTime

data class Adjudication (
  val adjudicationNumber: Long,
  val reportTime: LocalDateTime? = null,
  val agencyIncidentId: Long,
  val agencyId: String? = null,
  val partySeq: Long,
)
