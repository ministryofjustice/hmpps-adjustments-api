package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import java.util.UUID

@Service
class AdjustmentsService(
  private val adjustmentsDomainEventService: AdjustmentsDomainEventService,
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService,
) {

  @Transactional
  fun create(adjustments: List<AdjustmentDto>): CreateResponseDto = adjustmentsTransactionalService.create(adjustments).also { response ->
    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
      override fun afterCommit() {
        adjustmentsDomainEventService.create(response.adjustmentIds, adjustments[0].person, AdjustmentSource.DPS, adjustments[0].adjustmentType)
      }
    })
  }

  @Transactional
  fun updateEffectiveDays(adjustmentId: UUID, adjustment: AdjustmentEffectiveDaysDto) {
    adjustmentsTransactionalService.updateEffectiveDays(adjustmentId, adjustment).also {
      TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() {
          adjustmentsDomainEventService.updateEffectiveDays(adjustmentId, adjustment.person, AdjustmentSource.DPS)
        }
      })
    }
  }

  fun get(adjustmentId: UUID): AdjustmentDto = adjustmentsTransactionalService.get(adjustmentId)

  fun findCurrentAdjustments(
    person: String,
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    recallId: UUID? = null,
  ): List<AdjustmentDto> = adjustmentsTransactionalService.findCurrentAdjustments(person, status, currentPeriodOfCustody, recallId)

  @Transactional
  fun update(adjustmentId: UUID, adjustment: AdjustmentDto) {
    adjustmentsTransactionalService.update(adjustmentId, adjustment).also {
      TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() {
          adjustmentsDomainEventService.update(adjustmentId, adjustment.person, AdjustmentSource.DPS, adjustment.adjustmentType)
        }
      })
    }
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    adjustmentsTransactionalService.get(adjustmentId).also {
      adjustmentsTransactionalService.delete(adjustmentId)
      TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() {
          adjustmentsDomainEventService.delete(adjustmentId, it.person, AdjustmentSource.DPS, it.adjustmentType)
        }
      })
    }
  }

  @Transactional
  fun restore(adjustments: RestoreAdjustmentsDto) {
    adjustmentsTransactionalService.restore(adjustments).also {
      TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() {
          adjustmentsDomainEventService.create(adjustments.ids, it[0].person, AdjustmentSource.DPS, it[0].adjustmentType)
        }
      })
    }
  }
}
