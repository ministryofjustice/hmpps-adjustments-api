package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Create/ edit an adjustment")
data class EditableAdjustmentDto(
  @Schema(description = "The ID of the adjustment")
  val id: UUID?,
  @Schema(description = "The NOMIS booking ID of the adjustment")
  val bookingId: Long,
  @Schema(description = "The NOMIS ID of the person this adjustment applies to")
  val person: String,
  @Schema(description = "The type of adjustment")
  val adjustmentType: AdjustmentType,
  @Schema(description = "The end date of the adjustment")
  val toDate: LocalDate?,
  @Schema(description = "The start date of the adjustment")
  val fromDate: LocalDate?,
  @Schema(description = "The number of adjustment days")
  val days: Int?,
  @Schema(description = "The details of a remand adjustment")
  val remand: RemandDto?,
  @Schema(description = "The details of an additional days awarded adjustments (ADA)")
  val additionalDaysAwarded: AdditionalDaysAwardedDto?,
  @Schema(description = "Additional details of a UAL adjustment")
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
  @Schema(description = "The details of a tagged-bail adjustment")
  val taggedBail: TaggedBailDto?,
  @Schema(description = "The prison where the prisoner was located at the time the adjustment was created (a 3 character code identifying the prison)", example = "LDS", required = true)
  val prisonId: String? = null,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int? = null,
)
