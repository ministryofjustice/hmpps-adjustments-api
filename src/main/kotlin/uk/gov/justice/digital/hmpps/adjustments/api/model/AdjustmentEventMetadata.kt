package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.util.UUID

data class AdjustmentEventMetadata(
  val ids: List<UUID>,
  val person: String,
  val source: AdjustmentSource,
  val adjustmentType: AdjustmentType? = null,
)
