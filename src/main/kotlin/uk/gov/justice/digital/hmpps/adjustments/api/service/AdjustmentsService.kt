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
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.error.ApiValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDetailsDto
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
  fun create(resource: AdjustmentDetailsDto): CreateResponseDto {
    if (resource.toDate == null && resource.days == null) {
      throw ApiValidationException("resource must have either toDate or days.")
    }
    val daysCalculated: Int? = if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    val adjustment = Adjustment(
      person = resource.person,
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

  fun get(adjustmentId: UUID): AdjustmentDetailsDto {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.deleted) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    return mapToDto(adjustment)
  }

  fun findByPerson(person: String): List<AdjustmentDto> {
    return adjustmentRepository.findByPerson(person)
      .filter { !it.deleted }
      .map { AdjustmentDto(it.id, mapToDto(it)) }
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: AdjustmentDetailsDto) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    if (adjustment.adjustmentType != resource.adjustmentType) {
      throw ApiValidationException("The provided adjustment type ${resource.adjustmentType} doesn't match the persisted type ${adjustment.adjustmentType}")
    }
    val persistedLegacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val change = objectToJson(adjustment.copy(adjustmentHistory = emptyList()))
    var daysCalculated: Int? = if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    adjustment.apply {
      daysCalculated = resource.days ?: daysCalculated!!
      days = resource.days
      fromDate = resource.fromDate
      toDate = resource.toDate
      source = AdjustmentSource.DPS
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, null, persistedLegacyData.type, true))
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.DPS,
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
    val change = objectToJson(adjustment.copy(adjustmentHistory = emptyList()))
    adjustment.apply {
      deleted = true
      source = AdjustmentSource.DPS
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.DPS,
        change = change,
        adjustment = adjustment
      )
    }
  }

  private fun objectToJson(subject: Any): JsonNode {
    return JacksonUtil.toJsonNode(objectMapper.writeValueAsString(subject))
  }

  private fun mapToDto(adjustment: Adjustment): AdjustmentDetailsDto {
    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    return AdjustmentDetailsDto(
      person = adjustment.person,
      days = adjustment.days ?: adjustment.daysCalculated,
      fromDate = adjustment.fromDate!!,
      toDate = adjustment.toDate,
      adjustmentType = adjustment.adjustmentType,
      sentenceSequence = legacyData.sentenceSequence,
      bookingId = legacyData.bookingId
    )
  }
}
