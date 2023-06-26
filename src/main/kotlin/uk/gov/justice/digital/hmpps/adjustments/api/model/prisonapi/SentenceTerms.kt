package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

data class SentenceTerms(
  val years: Int = 0,
  val months: Int = 0,
  val weeks: Int = 0,
  val days: Int = 0,
  val code: String = "",
)
