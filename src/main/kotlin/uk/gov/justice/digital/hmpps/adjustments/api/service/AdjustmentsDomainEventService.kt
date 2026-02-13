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

  fun raiseAdjustmentEvent(event: AdjustmentEventMetadata) {

    val isUnusedDeductions = event.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS
    val additionalInformation = AdditionalInformation(
      event.ids[0],
      event.person,
      event.source.toString(),
      isUnusedDeductions,
      lastEvent = event.isLast
    )

    if (event.eventType == AdjustmentEventType.ADJUSTMENT_UPDATED_EFFECTIVE_DAYS) {
      additionalInformation.unusedDeductions = true
    }

    snsService.publishDomainEvent(
      event.eventType,
      LocalDateTime.now(),
      additionalInformation,
    )
  }
}
