package uk.gov.justice.digital.hmpps.adjustments.api.model.previousual

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "A request to confirm previous UAL adjustments")
data class PreviousUnlawfullyAtLargeReviewRequest(
  @param:Schema(description = "The IDs of any adjustments that should be recreated for the latest period of custody")
  val acceptedAdjustmentIds: List<UUID>,
  @param:Schema(description = "The IDs of any adjustments that should not be recreated or referenced again in future reviews")
  val rejectedAdjustmentIds: List<UUID>,
)
