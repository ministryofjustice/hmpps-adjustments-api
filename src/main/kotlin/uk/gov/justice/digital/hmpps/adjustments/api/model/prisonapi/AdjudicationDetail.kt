package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDateTime

data class AdjudicationDetail(
  val adjudicationNumber: Long,
  val hearings: List<Hearing>? = null,
  val incidentTime: LocalDateTime? = null,
  val establishment: String? = null,
  val interiorLocation: String? = null,
  val incidentDetails: String? = null,
  val reportNumber: Long? = null,
  val reportType: String? = null,
  val reporterFirstName: String? = null,
  val reporterLastName: String? = null,
  val reportTime: LocalDateTime? = null,
)
