package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

import java.time.LocalDate

data class LegacyAdjustment(
  val bookingId: Long,
  val sentenceSequence: Int,
  val adjustmentType: LegacyAdjustmentType,
  val adjustmentDate: LocalDate,
  val adjustmentFromDate: LocalDate?,
  val adjustmentDays: Int,
  val comment: String?,
  val active: Boolean
)