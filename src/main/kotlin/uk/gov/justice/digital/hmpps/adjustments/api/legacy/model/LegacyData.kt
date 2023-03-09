package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

data class LegacyData(
  val bookingId: Long = -1,
  val sentenceSequence: Int? = null,
  val comment: String? = null,
  val type: LegacyAdjustmentType? = null,
  val active: Boolean = false
)
