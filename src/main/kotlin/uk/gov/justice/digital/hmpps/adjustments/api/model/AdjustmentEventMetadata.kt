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
)

enum class AdjustmentEventType(val value: String) {
  ADJUSTMENT_CREATED("release-date-adjustments.adjustment.inserted"),
  ADJUSTMENT_UPDATED("release-date-adjustments.adjustment.updated"),
  ADJUSTMENT_UPDATED_EFFECTIVE_DAYS("release-date-adjustments.adjustment.updated"),
  ADJUSTMENT_DELETED("release-date-adjustments.adjustment.deleted"),
}
