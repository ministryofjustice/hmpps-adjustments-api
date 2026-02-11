package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdjustmentsDomainEventService(
  private val snsService: SnsService,
) {

  fun create(ids: List<UUID>, person: String, source: AdjustmentSource, adjustmentType: AdjustmentType? = null) {
    ids.forEachIndexed { index, it ->
      snsService.publishDomainEvent(
        EventType.ADJUSTMENT_CREATED,
        "An adjustment has been created",
        LocalDateTime.now(),
        AdditionalInformation(
          it,
          person,
          source.toString(),
          adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
          lastEvent = (ids.size - 1) == index,
        ),
      )
    }
  }

  fun update(id: UUID, person: String, source: AdjustmentSource, adjustmentType: AdjustmentType? = null) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_UPDATED,
      "An adjustment has been updated",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
        adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
      ),
    )
  }

  fun updateEffectiveDays(id: UUID, person: String, source: AdjustmentSource) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_UPDATED,
      "An adjustment's effective calculation days has been updated",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
        true,
      ),
    )
  }

  fun delete(id: UUID?, person: String, source: AdjustmentSource, adjustmentType: AdjustmentType? = null) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_DELETED,
      "An adjustment has been deleted",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
        adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS,
      ),
    )
  }
}
