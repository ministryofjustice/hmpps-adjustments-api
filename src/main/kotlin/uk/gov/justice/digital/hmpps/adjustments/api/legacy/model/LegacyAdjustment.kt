package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "An adjustment structured for synchronising with the NOMIS system")
data class LegacyAdjustment(
  @Schema(description = "The NOMIS booking ID of the adjustment")
  val bookingId: Long,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int?,
  @Schema(description = "The NOMIS offender number aka nomsId, prisonerId of the person this adjustment applies to")
  val offenderNo: String,
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
  val active: Boolean,
  @Schema(description = "Has the prisoner been released from the NOMIS booking")
  @Deprecated("This parameter is no longer used to determine if an adjustment is active.")
  val bookingReleased: Boolean,
  @Schema(description = "The ID of the agency the prisoner is located")
  val agencyId: String?,
  @Schema(description = "Is the adjustment part of the current term. (Most recent booking NOMIS)")
  val currentTerm: Boolean,
)
