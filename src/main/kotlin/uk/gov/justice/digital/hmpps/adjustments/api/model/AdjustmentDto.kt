package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "The adjustment and its identifier")
data class AdjustmentDto(
  @Schema(description = "The ID of the adjustment")
  val id: UUID?,
  @Schema(description = "The NOMIS booking ID of the adjustment")
  val bookingId: Long,
  @Schema(description = "The NOMIS ID of the person this adjustment applies to")
  val person: String,
  @Schema(description = "The type of adjustment")
  val adjustmentType: AdjustmentType,
  @Schema(description = "Human readable text for type of adjustment")
  val adjustmentTypeText: String? = null,
  @Schema(description = "The end date of the adjustment")
  val toDate: LocalDate?,
  @Schema(description = "The start date of the adjustment")
  val fromDate: LocalDate?,
  @Schema(description = "The details of a remand adjustment")
  val remand: RemandDto?,
  @Schema(description = "The details of an additional days awarded adjustments (ADA)")
  val additionalDaysAwarded: AdditionalDaysAwardedDto?,
  @Schema(description = "Additional details of a UAL adjustment")
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
  @Schema(description = "The details of a tagged-bail adjustment")
  val taggedBail: TaggedBailDto?,
  @Schema(description = "The prison where the prisoner was located at the time the adjustment was created (a 3 character code identifying the prison)", example = "LDS")
  val prisonId: String? = null,
  @Schema(description = "The name name of the prison where the prisoner was located at the time the adjustment was created ", example = "Leeds")
  val prisonName: String? = null,
  @Schema(description = "The person last updating this adjustment")
  val lastUpdatedBy: String,
  @Schema(description = "The status of this adjustment")
  val status: AdjustmentStatus,
  @Schema(description = "The date and time this adjustment was last updated")
  val lastUpdatedDate: LocalDateTime,
  @Schema(description = "The date and time this adjustment was last created")
  val createdDate: LocalDateTime,
  @Schema(description = "The number of days effective in a calculation. (for example remand minus any unused deductions)")
  val effectiveDays: Int,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int? = null,
  @Schema(description = "The total number of adjustment days")
  val daysTotal: Int,
)
