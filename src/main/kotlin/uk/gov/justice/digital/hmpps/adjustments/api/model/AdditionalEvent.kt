package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import java.util.UUID

data class AdditionalEvent(
  val eventType: EventType,
  val id: UUID,
)
