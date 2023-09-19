package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

import java.time.LocalDate

data class LegacyData(
  val bookingId: Long = -1,
  val sentenceSequence: Int? = null,
  val postedDate: LocalDate? = null,
  val comment: String? = null,
  val type: LegacyAdjustmentType? = null,
  val migration: Boolean = false,
)
