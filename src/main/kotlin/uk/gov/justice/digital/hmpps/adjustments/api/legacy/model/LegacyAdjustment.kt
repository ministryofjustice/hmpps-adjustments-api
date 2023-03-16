package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "An adjustment structured for synchronising with the NOMIS system")
data class LegacyAdjustment(
  @Schema(description = "The NOMIS booking ID of the adjustment")
  val bookingId: Long,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int?,
  @Schema(description = "The NOMIS ID of the person this adjustment applies to")
  val offenderId: String,
  @Schema(description = "The NOMIS adjustment type")
  val adjustmentType: LegacyAdjustmentType,
  @Schema(description = "The NOMIS date of adjustment")
  val adjustmentDate: LocalDate?,
  @Schema(description = "The NOMIS from date of adjustment")
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "The NOMIS adjustment days")
  val adjustmentDays: Int,
  @Schema(description = "The NOMIS comment for this adjustment")
  val comment: String?,
  @Schema(description = "The NOMIS active or inactive flag")
  val active: Boolean
)
