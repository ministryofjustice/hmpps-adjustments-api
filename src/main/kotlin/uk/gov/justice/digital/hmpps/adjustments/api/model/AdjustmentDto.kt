package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import java.time.LocalDate

data class AdjustmentDto(
  val bookingId: Long,
  val sentenceSequence: Int?,
  val offenderId: String,
  val adjustmentType: AdjustmentType,
  val toDate: LocalDate?,
  val fromDate: LocalDate,
  val days: Int?
)