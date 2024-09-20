package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ArithmeticType
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
  @Schema(description = "The end date of the adjustment")
  val toDate: LocalDate?,
  @Schema(description = "The start date of the adjustment")
  val fromDate: LocalDate?,
  @Schema(description = "The number of days of the adjustment")
  val days: Int?,
  @Schema(description = "The details of a remand adjustment")
  val remand: RemandDto?,
  @Schema(description = "The details of an additional days awarded adjustments (ADA)")
  val additionalDaysAwarded: AdditionalDaysAwardedDto?,
  @Schema(description = "Additional details of a UAL adjustment")
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
  @Schema(description = "Additional info for a LAL adjustment")
  val lawfullyAtLarge: LawfullyAtLargeDto?,
  @Schema(description = "The details of a tagged-bail adjustment")
  val taggedBail: TaggedBailDto?,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int? = null,

  // View only fields
  @Schema(description = "Human readable text for type of adjustment", readOnly = true)
  val adjustmentTypeText: String? = null,
  @Schema(description = "Indicates whether the adjustment was an addition or deduction", readOnly = true)
  val adjustmentArithmeticType: ArithmeticType? = null,
  @Schema(description = "The name name of the prison where the prisoner was located at the time the adjustment was created ", example = "Leeds", readOnly = true)
  val prisonName: String? = null,
  @Schema(description = "The prison where the prisoner was located at the time the adjustment was created (a 3 character code identifying the prison)", example = "LDS", readOnly = true)
  val prisonId: String? = null,
  @Schema(description = "The person last updating this adjustment", readOnly = true)
  val lastUpdatedBy: String? = null,
  @Schema(description = "The status of this adjustment", readOnly = true)
  val status: AdjustmentStatus? = null,
  @Schema(description = "The date and time this adjustment was last updated", readOnly = true)
  val lastUpdatedDate: LocalDateTime? = null,
  @Schema(description = "The date and time this adjustment was last created", readOnly = true)
  val createdDate: LocalDateTime? = null,
  @Schema(description = "The number of days effective in a calculation. (for example remand minus any unused deductions)", readOnly = true)
  val effectiveDays: Int? = null,
  @Schema(description = "Where was the adjustment last changed", readOnly = true)
  val source: AdjustmentSource? = null,
)
