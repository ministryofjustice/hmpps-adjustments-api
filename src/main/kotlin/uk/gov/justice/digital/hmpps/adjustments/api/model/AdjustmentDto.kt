package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDate
import java.util.UUID

@Schema(description = "The adjustment and its identifier")
data class AdjustmentDto(
  @Schema(description = "The ID of the adjustment")
  val id: UUID?,
  @Schema(description = "The NOMIS booking ID of the adjustment")
  val bookingId: Long,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int?,
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
  @Schema(description = "The details of an additional days awarded adjustments (ADA)")
  val additionalDaysAwarded: AdditionalDaysAwardedDto?,
  // View only fields
  @Schema(description = "The person last updating this adjustment", readOnly = true)
  val lastUpdatedBy: String? = null,
  @Schema(description = "The status of this adjustment", readOnly = true)
  val status: String? = null,
)
