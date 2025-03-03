package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import java.time.LocalDate
import java.util.UUID

@Service
class AdjustmentsService(
  private val adjustmentsDomainEventService: AdjustmentsDomainEventService,
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService,
) {

  fun create(adjustments: List<AdjustmentDto>): CreateResponseDto {
    return adjustmentsTransactionalService.create(adjustments).also {
      adjustmentsDomainEventService.create(it.adjustmentIds, adjustments[0].person, AdjustmentSource.DPS, adjustments[0].adjustmentType)
    }
  }

  fun updateEffectiveDays(adjustmentId: UUID, adjustment: AdjustmentEffectiveDaysDto) {
    adjustmentsTransactionalService.updateEffectiveDays(adjustmentId, adjustment).also {
      adjustmentsDomainEventService.updateEffectiveDays(adjustmentId, adjustment.person, AdjustmentSource.DPS)
    }
  }

  fun get(adjustmentId: UUID): AdjustmentDto {
    return adjustmentsTransactionalService.get(adjustmentId)
  }

  fun findCurrentAdjustments(
    person: String,
    status: AdjustmentStatus,
    currentPeriodOfCustody: Boolean,
    startOfSentenceEnvelope: LocalDate?,
    recallId: UUID? = null,
  ): List<AdjustmentDto> {
    return adjustmentsTransactionalService.findCurrentAdjustments(person, status, currentPeriodOfCustody, startOfSentenceEnvelope, recallId)
  }

  fun update(adjustmentId: UUID, adjustment: AdjustmentDto) {
    adjustmentsTransactionalService.update(adjustmentId, adjustment).also {
      adjustmentsDomainEventService.update(adjustmentId, adjustment.person, AdjustmentSource.DPS, adjustment.adjustmentType)
    }
  }

  fun delete(adjustmentId: UUID) {
    adjustmentsTransactionalService.get(adjustmentId).also {
      adjustmentsTransactionalService.delete(adjustmentId)
      adjustmentsDomainEventService.delete(adjustmentId, it.person, AdjustmentSource.DPS, it.adjustmentType)
    }
  }

  fun restore(adjustments: RestoreAdjustmentsDto) {
    adjustmentsTransactionalService.restore(adjustments).also {
      adjustmentsDomainEventService.create(adjustments.ids, it[0].person, AdjustmentSource.DPS, it[0].adjustmentType)
    }
  }
}
