package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdjustmentsEventService(
  private val snsService: SnsService,
) {

  fun create(id: UUID, person: String, source: AdjustmentSource) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_CREATED,
      "An adjustment has been created",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
      ),
    )
  }

  fun update(id: UUID, person: String, source: AdjustmentSource) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_UPDATED,
      "An adjustment has been updated",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
      ),
    )
  }

  fun delete(id: UUID, person: String, source: AdjustmentSource) {
    snsService.publishDomainEvent(
      EventType.ADJUSTMENT_DELETED,
      "An adjustment has been deleted",
      LocalDateTime.now(),
      AdditionalInformation(
        id,
        person,
        source.toString(),
      ),
    )
  }
}
