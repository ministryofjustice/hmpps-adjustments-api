package uk.gov.justice.digital.hmpps.adjustments.api.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjudicationCharges
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.error.ApiValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdjustmentsService(
  val adjustmentRepository: AdjustmentRepository,
  val objectMapper: ObjectMapper,
  private val prisonService: PrisonService,
  private val prisonApiClient: PrisonApiClient,
) {

  fun getCurrentAuthentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw IllegalStateException("User is not authenticated")

  @Transactional
  fun create(resource: AdjustmentDto): CreateResponseDto {
    if (resource.toDate == null && resource.days == null) {
      throw ApiValidationException("resource must have either toDate or days.")
    }
    val daysCalculated: Int? =
      if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    val adjustment = Adjustment(
      person = resource.person,
      daysCalculated = resource.days ?: daysCalculated!!,
      days = resource.days,
      fromDate = resource.fromDate,
      toDate = resource.toDate,
      source = AdjustmentSource.DPS,
      adjustmentType = resource.adjustmentType,
      prisonId = resource.prisonId,
      status = ACTIVE,
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          resource.sentenceSequence,
          LocalDate.now(),
          null,
          null,
        ),
      ),
      adjudicationCharges = adjudicationCharges(resource),
      unlawfullyAtLarge = unlawfullyAtLarge(resource),
    )
    adjustment.adjustmentHistory = listOf(
      AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.CREATE,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
      ),
    )
    return CreateResponseDto(adjustmentRepository.save(adjustment).id)
  }

  private fun unlawfullyAtLarge(adjustmentDto: AdjustmentDto, adjustment: Adjustment? = null): UnlawfullyAtLarge? =
    if (adjustmentDto.adjustmentType == UNLAWFULLY_AT_LARGE && adjustmentDto.unlawfullyAtLarge != null) {
      getUnlawfullyAtLarge(adjustment).apply {
        type = adjustmentDto.unlawfullyAtLarge.type
      }
    } else {
      null
    }

  private fun getUnlawfullyAtLarge(adjustment: Adjustment?) =
    if (adjustment?.unlawfullyAtLarge != null) {
      adjustment.unlawfullyAtLarge!!
    } else if (adjustment != null) {
      UnlawfullyAtLarge(adjustment = adjustment)
    } else {
      UnlawfullyAtLarge()
    }

  private fun adjudicationCharges(resource: AdjustmentDto): MutableList<AdjudicationCharges> {
    if (resource.adjustmentType == AdjustmentType.ADDITIONAL_DAYS_AWARDED && resource.additionalDaysAwarded != null) {
      return resource.additionalDaysAwarded.adjudicationId.map { AdjudicationCharges(it) }.toMutableList()
    }
    return mutableListOf()
  }

  fun get(adjustmentId: UUID): AdjustmentDto {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status == DELETED) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    return mapToDto(adjustment)
  }

  fun findCurrentAdjustments(person: String, startOfSentenceEnvelope: LocalDate? = null): List<AdjustmentDto> {
    val fromDate = startOfSentenceEnvelope ?: prisonService.getStartOfSentenceEnvelope(person)
    return adjustmentRepository.findCurrentAdjustmentsByPerson(person, fromDate).map { mapToDto(it) }
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: AdjustmentDto) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    if (adjustment.adjustmentType != resource.adjustmentType) {
      throw ApiValidationException("The provided adjustment type ${resource.adjustmentType} doesn't match the persisted type ${adjustment.adjustmentType}")
    }
    val persistedLegacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val change = objectToJson(adjustment)
    val calculated: Int? =
      if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    adjustment.apply {
      daysCalculated = resource.days ?: calculated!!
      days = resource.days
      fromDate = resource.fromDate
      toDate = resource.toDate
      source = AdjustmentSource.DPS
      prisonId = resource.prisonId
      status = ACTIVE
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          resource.sentenceSequence,
          persistedLegacyData.postedDate,
          persistedLegacyData.comment,
          persistedLegacyData.type,
        ),
      )
      adjudicationCharges = adjudicationCharges(resource)
      unlawfullyAtLarge = unlawfullyAtLarge(resource, this)
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
      )
    }
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val change = objectToJson(adjustment)
    adjustment.apply {
      status = DELETED
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.DPS,
        change = change,
        adjustment = adjustment,
      )
    }
  }

  private fun objectToJson(subject: Any): JsonNode {
    return JacksonUtil.toJsonNode(objectMapper.writeValueAsString(subject))
  }

  private fun mapToDto(adjustment: Adjustment): AdjustmentDto {
    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val prisonDescription = adjustment.prisonId?.let { prisonApiClient.getPrison(adjustment.prisonId!!).description }
    return AdjustmentDto(
      id = adjustment.id,
      person = adjustment.person,
      days = adjustment.days ?: adjustment.daysCalculated,
      fromDate = adjustment.fromDate,
      toDate = adjustment.toDate,
      adjustmentType = adjustment.adjustmentType,
      sentenceSequence = legacyData.sentenceSequence,
      bookingId = legacyData.bookingId,
      additionalDaysAwarded = additionalDaysAwardedToDto(adjustment),
      unlawfullyAtLarge = unlawfullyAtLargeDto(adjustment),
      lastUpdatedBy = adjustment.adjustmentHistory.last().changeByUsername,
      lastUpdatedDate = adjustment.adjustmentHistory.last().changeAt,
      status = adjustment.status,
      prisonId = adjustment.prisonId,
      prisonName = prisonDescription,
    )
  }

  private fun unlawfullyAtLargeDto(adjustment: Adjustment): UnlawfullyAtLargeDto? =
    if (adjustment.unlawfullyAtLarge != null) {
      UnlawfullyAtLargeDto(type = adjustment.unlawfullyAtLarge!!.type)
    } else {
      null
    }

  private fun additionalDaysAwardedToDto(adjustment: Adjustment): AdditionalDaysAwardedDto? {
    if (adjustment.adjudicationCharges.isNotEmpty()) {
      return AdditionalDaysAwardedDto(
        adjudicationId = adjustment.adjudicationCharges.map { it.adjudicationId },
      )
    }
    return null
  }
}
