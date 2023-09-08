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
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdditionalDaysAwarded
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Remand
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.error.ApiValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalEvent
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
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
    val remand = remand(resource)
    val adjustment = Adjustment(
      person = resource.person,
      daysCalculated = resource.days ?: daysCalculated!!,
      days = if (resource.remand == null) resource.days else daysCalculated!!.minus(resource.remand.unusedDays),
      fromDate = resource.fromDate,
      toDate = resource.toDate,
      source = AdjustmentSource.DPS,
      adjustmentType = resource.adjustmentType,
      prisonId = resource.prisonId,
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          resource.sentenceSequence,
          LocalDate.now(),
          null,
          null,
          true,
        ),
      ),
      additionalDaysAwarded = additionalDaysAwarded(resource),
      unlawfullyAtLarge = unlawfullyAtLarge(resource),
      remand = remand?.first,
    )
    adjustment.adjustmentHistory = listOf(
      AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.CREATE,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
      ),
    )
    return CreateResponseDto(adjustmentRepository.save(adjustment).id, remand?.second)
  }

  private fun remand(resource: AdjustmentDto, adjustment: Adjustment? = null): Pair<Remand?, AdditionalEvent>? {
    if (resource.remand != null) {
      if (adjustment?.remand == null) {
        var unusedRemandAdjustment = Adjustment(
          person = resource.person,
          days = resource.remand.unusedDays,
          fromDate = resource.fromDate,
          source = AdjustmentSource.DPS,
          adjustmentType = resource.adjustmentType,
          prisonId = resource.prisonId,
          legacyData = objectToJson(
            LegacyData(
              resource.bookingId,
              null,
              LocalDate.now(),
              null,
              LegacyAdjustmentType.UR,
              true,
            ),
          ),
        )
        unusedRemandAdjustment.adjustmentHistory = listOf(
          AdjustmentHistory(
            changeByUsername = getCurrentAuthentication().principal,
            changeType = ChangeType.CREATE,
            changeSource = AdjustmentSource.DPS,
            adjustment = unusedRemandAdjustment,
          ),
        )
        unusedRemandAdjustment = adjustmentRepository.save(unusedRemandAdjustment)
        return Remand(unusedRemand = unusedRemandAdjustment) to AdditionalEvent(EventType.ADJUSTMENT_CREATED, unusedRemandAdjustment.id)
      } else {
        adjustment.remand!!.unusedRemand.days = resource.remand.unusedDays
        return adjustment.remand!! to AdditionalEvent(EventType.ADJUSTMENT_UPDATED, adjustment.remand!!.unusedRemand.id)
      }
    } else if (adjustment?.remand != null) {
      val change = objectToJson(adjustment.remand!!.unusedRemand)
      adjustment.remand!!.unusedRemand.apply {
        deleted = true
        adjustmentHistory += AdjustmentHistory(
          changeByUsername = getCurrentAuthentication().principal,
          changeType = ChangeType.DELETE,
          changeSource = AdjustmentSource.DPS,
          change = change,
          adjustment = this,
        )
      }
      return null to AdditionalEvent(EventType.ADJUSTMENT_DELETED, adjustment.remand!!.unusedRemand.id)
    }
    return null
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

  private fun additionalDaysAwarded(resource: AdjustmentDto, adjustment: Adjustment? = null): AdditionalDaysAwarded? {
    if (resource.adjustmentType == AdjustmentType.ADDITIONAL_DAYS_AWARDED && resource.additionalDaysAwarded != null) {
      val additionalDaysAwarded =
        if (adjustment != null) adjustment.additionalDaysAwarded!! else AdditionalDaysAwarded()
      additionalDaysAwarded.apply {
        adjudicationId = resource.additionalDaysAwarded.adjudicationId
        consecutive = resource.additionalDaysAwarded.consecutive
      }
      return additionalDaysAwarded
    }
    return null
  }

  fun get(adjustmentId: UUID): AdjustmentDto {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.deleted) null else it }
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
  fun update(adjustmentId: UUID, resource: AdjustmentDto): AdditionalEvent? {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    if (adjustment.adjustmentType != resource.adjustmentType) {
      throw ApiValidationException("The provided adjustment type ${resource.adjustmentType} doesn't match the persisted type ${adjustment.adjustmentType}")
    }
    val persistedLegacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val change = objectToJson(adjustment)
    val remandAndEvent = remand(resource, adjustment)
    val calculated: Int? =
      if (resource.toDate != null) (ChronoUnit.DAYS.between(resource.fromDate, resource.toDate) + 1).toInt() else null
    adjustment.apply {
      daysCalculated = resource.days ?: calculated!!
      days = if (resource.remand == null) resource.days else calculated!!.minus(resource.remand.unusedDays)
      fromDate = resource.fromDate
      toDate = resource.toDate
      source = AdjustmentSource.DPS
      prisonId = resource.prisonId
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          resource.sentenceSequence,
          persistedLegacyData.postedDate,
          persistedLegacyData.comment,
          persistedLegacyData.type,
          true,
        ),
      )
      additionalDaysAwarded = additionalDaysAwarded(resource, this)
      unlawfullyAtLarge = unlawfullyAtLarge(resource, this)
      remand = remandAndEvent?.first
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
      )
    }
    return remandAndEvent?.second
  }

  @Transactional
  fun delete(adjustmentId: UUID): AdditionalEvent? {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val change = objectToJson(adjustment)
    adjustment.apply {
      deleted = true
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthentication().principal,
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.DPS,
        change = change,
        adjustment = adjustment,
      )
    }
    if (adjustment.remand?.unusedRemand !== null) {
      val unusedRemandChange = objectToJson(adjustment.remand!!.unusedRemand)
      adjustment.remand!!.unusedRemand.apply {
        deleted = true
        adjustmentHistory += AdjustmentHistory(
          changeByUsername = getCurrentAuthentication().principal,
          changeType = ChangeType.DELETE,
          changeSource = AdjustmentSource.DPS,
          change = unusedRemandChange,
          adjustment = this,
        )
      }
      return AdditionalEvent(EventType.ADJUSTMENT_DELETED, adjustment.remand!!.unusedRemand.id)
    }
    return null
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
      remand = remandDto(adjustment),
      lastUpdatedBy = adjustment.adjustmentHistory.last().changeByUsername,
      lastUpdatedDate = adjustment.adjustmentHistory.last().changeAt,
      status = if (legacyData.active) "Active" else "Inactive",
      prisonId = adjustment.prisonId,
      prisonName = prisonDescription,
    )
  }

  private fun remandDto(adjustment: Adjustment): RemandDto? {
    return if (adjustment.remand?.unusedRemand != null) {
      RemandDto(adjustment.remand!!.unusedRemand.days!!)
    } else {
      null
    }
  }

  private fun unlawfullyAtLargeDto(adjustment: Adjustment): UnlawfullyAtLargeDto? =
    if (adjustment.unlawfullyAtLarge != null) {
      UnlawfullyAtLargeDto(type = adjustment.unlawfullyAtLarge!!.type)
    } else {
      null
    }

  private fun additionalDaysAwardedToDto(adjustment: Adjustment): AdditionalDaysAwardedDto? {
    if (adjustment.additionalDaysAwarded != null) {
      return AdditionalDaysAwardedDto(
        adjudicationId = adjustment.additionalDaysAwarded!!.adjudicationId,
        consecutive = adjustment.additionalDaysAwarded!!.consecutive,
      )
    }
    return null
  }
}
