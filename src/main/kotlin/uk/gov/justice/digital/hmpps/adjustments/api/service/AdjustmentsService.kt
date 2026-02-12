package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RecordResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import java.util.UUID

@Service
class AdjustmentsService(
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService,
) {

  fun create(adjustments: List<AdjustmentDto>): RecordResponse<CreateResponseDto> {
    val result = adjustmentsTransactionalService.create(adjustments)
    return RecordResponse(
      result,
      AdjustmentEventMetadata(
        AdjustmentEventType.ADJUSTMENT_CREATED,
        result.adjustmentIds,
        adjustments[0].person,
        AdjustmentSource.DPS,
        adjustments[0].adjustmentType,
      ),
    )
  }

  fun updateEffectiveDays(adjustmentId: UUID, adjustment: AdjustmentEffectiveDaysDto): RecordResponse<AdjustmentEffectiveDaysDto> {
    adjustmentsTransactionalService.updateEffectiveDays(adjustmentId, adjustment)
    return RecordResponse(
      adjustment,
      AdjustmentEventMetadata(
        AdjustmentEventType.ADJUSTMENT_UPDATED_EFFECTIVE_DAYS,
        listOfNotNull(adjustmentId),
        adjustment.person,
        AdjustmentSource.DPS,
        null,
      ),
    )
  }

  fun get(adjustmentId: UUID): AdjustmentDto = adjustmentsTransactionalService.get(adjustmentId)

  fun findCurrentAdjustments(
    person: String,
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    recallId: UUID? = null,
  ): List<AdjustmentDto> = adjustmentsTransactionalService.findCurrentAdjustments(person, status, currentPeriodOfCustody, recallId)

  fun update(adjustmentId: UUID, adjustment: AdjustmentDto): RecordResponse<AdjustmentDto> {
    adjustmentsTransactionalService.update(adjustmentId, adjustment)
    return RecordResponse(
      adjustment,
      AdjustmentEventMetadata(
        AdjustmentEventType.ADJUSTMENT_UPDATED,
        listOfNotNull(adjustmentId),
        adjustment.person,
        AdjustmentSource.DPS,
        adjustment.adjustmentType,
      ),
    )
  }

  fun delete(adjustmentId: UUID): RecordResponse<AdjustmentDto> {
    val adjustment = adjustmentsTransactionalService.get(adjustmentId).also {
      adjustmentsTransactionalService.delete(adjustmentId)
    }

    return RecordResponse(
      adjustment,
      AdjustmentEventMetadata(
        AdjustmentEventType.ADJUSTMENT_DELETED,
        listOfNotNull(adjustmentId),
        adjustment.person,
        AdjustmentSource.DPS,
        adjustment.adjustmentType,
      ),
    )
  }

  fun restore(adjustments: RestoreAdjustmentsDto): RecordResponse<List<AdjustmentDto>> {
    val restoredAdjustment = adjustmentsTransactionalService.restore(adjustments)

    return RecordResponse(
      restoredAdjustment,
      AdjustmentEventMetadata(
        AdjustmentEventType.ADJUSTMENT_CREATED,
        adjustments.ids,
        restoredAdjustment[0].person,
        AdjustmentSource.DPS,
        restoredAdjustment[0].adjustmentType,
      ),
    )
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
