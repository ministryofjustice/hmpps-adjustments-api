package uk.gov.justice.digital.hmpps.adjustments.api.model.previousual

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import java.time.LocalDate
import java.util.UUID

@Schema(description = "A UAL adjustment from a previous period of custody")
data class PreviousUnlawfullyAtLargeAdjustmentForReview(
  @param:Schema(description = "The ID of the adjustment")
  val id: UUID,
  @param:Schema(description = "The start date of the UAL")
  val fromDate: LocalDate,
  @param:Schema(description = "The end date of the UAL")
  val toDate: LocalDate,
  @param:Schema(description = "The number of days of the adjustment")
  val days: Int,
  @param:Schema(description = "The type of UAL")
  val type: UnlawfullyAtLargeType?,
  @param:Schema(description = "The name name of the prison where the prisoner was located at the time the adjustment was created ", example = "Leeds", readOnly = true)
  val prisonName: String?,
  @param:Schema(description = "The prison where the prisoner was located at the time the adjustment was created (a 3 character code identifying the prison)", example = "LDS", readOnly = true)
  val prisonId: String?,
)
