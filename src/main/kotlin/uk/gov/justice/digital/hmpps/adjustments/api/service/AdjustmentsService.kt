package uk.gov.justice.digital.hmpps.adjustments.api.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.error.ValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.error.LegacyAdjustmentTypeMissmatch
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.persistence.EntityNotFoundException

@Service
class AdjustmentsService(
  val adjustmentRepository: AdjustmentRepository,
  val objectMapper: ObjectMapper
) {

  fun getCurrentAuthentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw IllegalStateException("User is not authenticated")

  @Transactional
  fun create(resource: AdjustmentDto): CreateResponseDto {
    if (resource.toDate == null && resource.days == null) {
      throw ValidationException("resource must have either toDate or days.")
    }
    val daysCalculated: Int? = if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    val adjustment = Adjustment(
      person = resource.offenderId,
      daysCalculated = resource.days ?: daysCalculated!!,
      days = resource.days,
      fromDate = resource.fromDate,
      toDate = resource.toDate,
      source = AdjustmentSource.DPS,
      adjustmentType = resource.adjustmentType,
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, null, null, true)),
      adjustmentHistory = listOf(
        AdjustmentHistory(
          changeByUsername = getCurrentAuthentication().principal,
          changeType = ChangeType.CREATE,
          changeSource = AdjustmentSource.DPS
        )
      )
    )

    return CreateResponseDto(adjustmentRepository.save(adjustment).id)
  }

  fun get(adjustmentId: UUID): AdjustmentDto {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.deleted) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    return AdjustmentDto(
      offenderId = adjustment.person,
      adjustmentDays = adjustment.daysCalculated,
      adjustmentFromDate = adjustment.fromDate,
      adjustmentDate = adjustment.toDate,
      adjustmentType = transform(adjustment.adjustmentType, legacyData),
      sentenceSequence = legacyData.sentenceSequence,
      bookingId = legacyData.bookingId,
      active = legacyData.active,
      comment = legacyData.comment
    )
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: LegacyAdjustment) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val persistedLegacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val persistedLegacyAdjustmentType = transform(adjustment.adjustmentType, persistedLegacyData)
    if (persistedLegacyAdjustmentType != resource.adjustmentType) {
      throw LegacyAdjustmentTypeMissmatch("The provided adjustment type ${resource.adjustmentType} doesn't match the persisted type $persistedLegacyAdjustmentType")
    }
    val change = objectToJson(adjustment.copy(adjustmentHistory = emptyList()))
    adjustment.apply {
      daysCalculated = resource.adjustmentDays
      days = resource.adjustmentDays
      fromDate = resource.adjustmentFromDate
      toDate = resource.adjustmentDate
      source = AdjustmentSource.NOMIS
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.comment, resource.adjustmentType, resource.active))
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = adjustment
      )
    }
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    adjustment.apply {
      deleted = true
      source = AdjustmentSource.NOMIS
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = adjustment
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
      LegacyAdjustmentType.UR -> AdjustmentType.REMAND
      LegacyAdjustmentType.LAL -> AdjustmentType.LAWFULLY_AT_LARGE
      LegacyAdjustmentType.SREM -> AdjustmentType.SPECIAL_REMISSION
    }
  }
}