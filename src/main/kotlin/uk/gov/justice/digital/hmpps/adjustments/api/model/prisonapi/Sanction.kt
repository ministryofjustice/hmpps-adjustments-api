package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

data class Sanction (
  val sanctionType: String? = null,
  // TODO check that sanction days is always populated in NOMIS
  val sanctionDays: Int,
  val status: String? = null,
  val sanctionSeq: Long? = null,
  val consecutiveSanctionSeq: Long? = null,
)
