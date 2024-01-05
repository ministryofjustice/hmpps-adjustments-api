package uk.gov.justice.digital.hmpps.adjustments.api.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE_WHEN_DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LegacyService(
  val adjustmentRepository: AdjustmentRepository,
  val objectMapper: ObjectMapper,
) {

  @Transactional
  fun create(resource: LegacyAdjustment, migration: Boolean): LegacyAdjustmentCreatedResponse {
    val adjustment = Adjustment(
      person = resource.offenderNo,
      effectiveDays = resource.adjustmentDays,
      fromDate = resource.adjustmentFromDate,
      toDate = resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1),
      source = AdjustmentSource.NOMIS,
      adjustmentType = transform(resource.adjustmentType),
      status = if (resource.active) ACTIVE else INACTIVE,
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, migration)),
      adjustmentHistory = listOf(
        AdjustmentHistory(
          changeByUsername = "NOMIS",
          changeType = ChangeType.CREATE,
          changeSource = AdjustmentSource.NOMIS,
        ),
      ),
    )

    return LegacyAdjustmentCreatedResponse(adjustmentRepository.save(adjustment).id)
  }

  fun get(adjustmentId: UUID): LegacyAdjustment {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status == DELETED || it.status == INACTIVE_WHEN_DELETED) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    return LegacyAdjustment(
      offenderNo = adjustment.person,
      adjustmentDays = adjustment.effectiveDays,
      adjustmentFromDate = adjustment.fromDate,
      adjustmentDate = legacyData.postedDate,
      adjustmentType = transform(adjustment.adjustmentType, legacyData),
      sentenceSequence = legacyData.sentenceSequence,
      bookingId = legacyData.bookingId,
      active = adjustment.status == ACTIVE,
      comment = legacyData.comment,
    )
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: LegacyAdjustment) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val change = objectToJson(adjustment)
    adjustment.apply {
      effectiveDays = resource.adjustmentDays
      fromDate = resource.adjustmentFromDate
      toDate = if (adjustmentHasDPSUnusedDeductions(this)) this.toDate else resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1)
      source = AdjustmentSource.NOMIS
      status = if (resource.active) ACTIVE else INACTIVE
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, false))
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = adjustment,
      )
    }
  }

  private fun adjustmentHasDPSUnusedDeductions(adjustment: Adjustment): Boolean {
    val dpsDays = adjustment.days ?: adjustment.daysCalculated
    return dpsDays != null && adjustment.effectiveDays != dpsDays
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val change = objectToJson(adjustment)
    adjustment.apply {
      status = if (this.status == INACTIVE) INACTIVE_WHEN_DELETED else DELETED
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.NOMIS,
        change = change,
        adjustment = adjustment,
      )
    }
  }

  fun objectToJson(subject: Any): JsonNode {
    return JacksonUtil.toJsonNode(objectMapper.writeValueAsString(subject))
  }

  fun transform(type: AdjustmentType, legacyData: LegacyData): LegacyAdjustmentType {
    return when (type) {
      AdjustmentType.UNLAWFULLY_AT_LARGE -> LegacyAdjustmentType.UAL
      AdjustmentType.REMAND -> legacyData.type ?: LegacyAdjustmentType.RX
      AdjustmentType.TAGGED_BAIL -> legacyData.type ?: LegacyAdjustmentType.S240A
      AdjustmentType.LAWFULLY_AT_LARGE -> LegacyAdjustmentType.LAL
      AdjustmentType.ADDITIONAL_DAYS_AWARDED -> LegacyAdjustmentType.ADA
      AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED -> LegacyAdjustmentType.RADA
      AdjustmentType.SPECIAL_REMISSION -> LegacyAdjustmentType.SREM
      AdjustmentType.UNUSED_DEDUCTIONS -> LegacyAdjustmentType.UR
    }
  }

  fun transform(type: LegacyAdjustmentType): AdjustmentType {
    return when (type) {
      LegacyAdjustmentType.ADA -> AdjustmentType.ADDITIONAL_DAYS_AWARDED
      LegacyAdjustmentType.RADA -> AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED
      LegacyAdjustmentType.UAL -> AdjustmentType.UNLAWFULLY_AT_LARGE
      LegacyAdjustmentType.RSR -> AdjustmentType.REMAND
      LegacyAdjustmentType.RST -> AdjustmentType.TAGGED_BAIL
      LegacyAdjustmentType.RX -> AdjustmentType.REMAND
      LegacyAdjustmentType.S240A -> AdjustmentType.TAGGED_BAIL
      LegacyAdjustmentType.UR -> AdjustmentType.UNUSED_DEDUCTIONS
      LegacyAdjustmentType.LAL -> AdjustmentType.LAWFULLY_AT_LARGE
      LegacyAdjustmentType.SREM -> AdjustmentType.SPECIAL_REMISSION
    }
  }
}
