package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.util.UUID

data class AdjustmentEventMetadata(
  val eventType: AdjustmentEventType,
  val ids: List<UUID>,
  val person: String,
  val source: AdjustmentSource,
  val adjustmentType: AdjustmentType? = null,
  var isLast: Boolean = true,
)

enum class AdjustmentEventType(val value: String, val desc: String) {
  ADJUSTMENT_CREATED("release-date-adjustments.adjustment.inserted", "An adjustment has been created"),
  ADJUSTMENT_UPDATED("release-date-adjustments.adjustment.updated", "An adjustment has been updated"),
  ADJUSTMENT_UPDATED_EFFECTIVE_DAYS("release-date-adjustments.adjustment.updated", desc = "An adjustment's effective calculation days has been updated"),
  ADJUSTMENT_DELETED("release-date-adjustments.adjustment.deleted", "An adjustment has been deleted"),
}
