package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import java.time.LocalDateTime

@Service
class AdjustmentsDomainEventService(
  private val snsService: SnsService,
) {

  fun raiseAdjustmentEvents(event: AdjustmentEventMetadata) {
    if (event.eventType == AdjustmentEventType.ADJUSTMENT_CREATED) {
      event.ids.forEachIndexed { index, it ->
        snsService.publishDomainEvent(
          AdjustmentEventType.ADJUSTMENT_CREATED,
          "An adjustment has been created",
          LocalDateTime.now(),
          AdditionalInformation(
            it,
            event.person,
            event.source.toString(),
            event.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
            lastEvent = (event.ids.size - 1) == index,
          ),
        )
      }
    } else if (event.eventType == AdjustmentEventType.ADJUSTMENT_UPDATED) {
      snsService.publishDomainEvent(
        AdjustmentEventType.ADJUSTMENT_UPDATED,
        "An adjustment has been updated",
        LocalDateTime.now(),
        AdditionalInformation(
          event.ids[0],
          event.person,
          event.source.toString(),
          event.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
        ),
      )
    } else if (event.eventType == AdjustmentEventType.ADJUSTMENT_DELETED) {
      snsService.publishDomainEvent(
        AdjustmentEventType.ADJUSTMENT_DELETED,
        "An adjustment has been deleted",
        LocalDateTime.now(),
        AdditionalInformation(
          event.ids[0],
          event.person,
          event.source.toString(),
          event.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
        ),
      )
    } else if (event.eventType == AdjustmentEventType.ADJUSTMENT_UPDATED_EFFECTIVE_DAYS) {
      snsService.publishDomainEvent(
        AdjustmentEventType.ADJUSTMENT_UPDATED_EFFECTIVE_DAYS,
        "An adjustment's effective calculation days has been updated",
        LocalDateTime.now(),
        AdditionalInformation(
          event.ids[0],
          event.person,
          event.source.toString(),
          true,
        ),
      )
    }
  }
}
