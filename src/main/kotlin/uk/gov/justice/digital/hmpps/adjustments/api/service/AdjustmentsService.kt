package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RecordResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import java.util.UUID

@Service
class AdjustmentsService(
  private val adjustmentsDomainEventService: AdjustmentsDomainEventService,
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService,
) {

  fun create(adjustments: List<AdjustmentDto>): RecordResponse<CreateResponseDto> {
    val result = adjustmentsTransactionalService.create(adjustments)
    return RecordResponse(
      result,
      AdjustmentEventMetadata(
        result.adjustmentIds,
        adjustments[0].person,
        AdjustmentSource.DPS,
        adjustments[0].adjustmentType,
      ),
    )
  }

  fun updateEffectiveDays(adjustmentId: UUID, adjustment: AdjustmentEffectiveDaysDto) {
    adjustmentsTransactionalService.updateEffectiveDays(adjustmentId, adjustment).also {
      adjustmentsDomainEventService.updateEffectiveDays(adjustmentId, adjustment.person, AdjustmentSource.DPS)
    }
  }

  fun get(adjustmentId: UUID): AdjustmentDto = adjustmentsTransactionalService.get(adjustmentId)

  fun findCurrentAdjustments(
    person: String,
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    recallId: UUID? = null,
  ): List<AdjustmentDto> = adjustmentsTransactionalService.findCurrentAdjustments(person, status, currentPeriodOfCustody, recallId)

  fun update(adjustmentId: UUID, adjustment: AdjustmentDto) {
    adjustmentsTransactionalService.update(adjustmentId, adjustment).also {
      adjustmentsDomainEventService.update(adjustmentId, adjustment.person, AdjustmentSource.DPS, adjustment.adjustmentType)
    }
  }

  fun delete(adjustmentId: UUID): RecordResponse<AdjustmentDto> {
    val adjustment = adjustmentsTransactionalService.get(adjustmentId)
    if (adjustment.id != null) {
      adjustmentsTransactionalService.delete(adjustment.id)
    }

    return RecordResponse(
      adjustment,
      AdjustmentEventMetadata(
        listOfNotNull(adjustment.id),
        adjustment.person,
        AdjustmentSource.DPS,
        adjustment.adjustmentType,
      ),
    )
  }

  fun restore(adjustments: RestoreAdjustmentsDto) {
    adjustmentsTransactionalService.restore(adjustments).also {
      adjustmentsDomainEventService.create(adjustments.ids, it[0].person, AdjustmentSource.DPS, it[0].adjustmentType)
    }
  }
}
