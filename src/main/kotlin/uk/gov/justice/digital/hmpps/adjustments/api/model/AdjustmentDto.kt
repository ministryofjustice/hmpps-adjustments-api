package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
  @Schema(description = "Additional details of a UAL adjustment")
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
  @Schema(description = "The prison where the prisoner was located at the time the adjustment was created (a 3 character code identifying the prison)", example = "LDS")
  val prisonId: String? = null,
  // View only fields
  @Schema(description = "The name name of the prison where the prisoner was located at the time the adjustment was created ", example = "Leeds", readOnly = true)
  val prisonName: String? = null,
  @Schema(description = "The person last updating this adjustment", readOnly = true)
  val lastUpdatedBy: String? = null,
  @Schema(description = "The status of this adjustment", readOnly = true)
  val status: AdjustmentStatus? = null,
  @Schema(description = "The date and time this adjustment was last updated", readOnly = true)
  val lastUpdatedDate: LocalDateTime? = null,
  @Schema(description = "The number of days effective in a calculation. (for example remand minus any unused deductions)")
  val effectiveDays: Int?,
) {


  val daysBetween: Int?
    get() {
      return if (this.fromDate == null || this.toDate == null) {
        null
      } else {
        (ChronoUnit.DAYS.between(this.fromDate, this.toDate) + 1).toInt()
      }
    }
}
