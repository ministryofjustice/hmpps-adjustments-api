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
import uk.gov.justice.digital.hmpps.adjustments.api.config.UserContext
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdditionalDaysAwarded
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjudicationCharges
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE_WHEN_DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.REMAND
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.TAGGED_BAIL
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.error.ApiValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.SentenceInfo
import uk.gov.justice.digital.hmpps.adjustments.api.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdjustmentsTransactionalService(
  val adjustmentRepository: AdjustmentRepository,
  val objectMapper: ObjectMapper,
  private val prisonService: PrisonService,
  private val prisonApiClient: PrisonApiClient,
) {

  fun getCurrentAuthenticationUsername(): String =
    (SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?)?.principal
      ?: UserContext.getOverrideUsername()

  @Transactional
  fun create(resource: List<AdjustmentDto>): CreateResponseDto {
    return CreateResponseDto(resource.map { create(it) })
  }

  private fun create(resource: AdjustmentDto): UUID {
    val isPeriodAdjustment = resource.fromDate != null && resource.toDate != null
    val daysBetween = daysBetween(resource.fromDate, resource.toDate)

    if (isPeriodAdjustment && resource.days != null) {
      if (daysBetween != resource.days) {
        throw ApiValidationException("The number of days provide does not match the period between the from and to dates of the adjustment")
      }
    }
    val sentenceInfo = sentenceInfo(resource)
    val prisoner = prisonApiClient.getPrisonerDetail(resource.person)
    val adjustment = Adjustment(
      person = resource.person,
      effectiveDays = daysBetween ?: resource.days!!,
      days = if (isPeriodAdjustment) null else resource.days,
      daysCalculated = daysBetween,
      fromDate = resource.fromDate,
      toDate = resource.toDate,
      source = AdjustmentSource.DPS,
      adjustmentType = resource.adjustmentType,
      status = ACTIVE,
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          sentenceInfo?.sentenceSequence,
          LocalDate.now(),
          null,
          legacyType(resource.adjustmentType, sentenceInfo),
          chargeIds = resource.remand?.chargeId ?: emptyList(),
          caseSequence = resource.taggedBail?.caseSequence,
        ),
      ),
      additionalDaysAwarded = additionalDaysAwarded(resource),
      unlawfullyAtLarge = unlawfullyAtLarge(resource),
    )
    adjustment.adjustmentHistory = listOf(
      AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.CREATE,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
        prisonId = prisoner.agencyId,
      ),
    )
    return adjustmentRepository.save(adjustment).id
  }

  @Transactional
  fun updateEffectiveDays(adjustmentId: UUID, effectiveDaysDto: AdjustmentEffectiveDaysDto) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    adjustment.apply {
      effectiveDays = effectiveDaysDto.effectiveDays
    }
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
      return getAdditionalDaysAwarded(adjustment).apply {
        adjudicationCharges =
          resource.additionalDaysAwarded.adjudicationId.map { AdjudicationCharges(it) }.toMutableList()
        prospective = resource.additionalDaysAwarded.prospective
      }
    }
    return null
  }

  private fun getAdditionalDaysAwarded(adjustment: Adjustment?) =
    if (adjustment?.additionalDaysAwarded != null) {
      adjustment.additionalDaysAwarded!!
    } else if (adjustment != null) {
      AdditionalDaysAwarded(adjustment = adjustment)
    } else {
      AdditionalDaysAwarded()
    }

  fun get(adjustmentId: UUID): AdjustmentDto {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status.isDeleted()) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    return mapToDto(adjustment)
  }

  fun findCurrentAdjustments(
    person: String,
    status: AdjustmentStatus,
    startOfSentenceEnvelope: LocalDate?,
  ): List<AdjustmentDto> {
    return if (startOfSentenceEnvelope != null) {
      adjustmentRepository.findAdjustmentsByPersonWithinSentenceEnvelope(person, startOfSentenceEnvelope, status)
    } else {
      adjustmentRepository.findByPersonAndStatus(person, status)
    }.map { mapToDto(it) }
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
    val isPeriodAdjustment = resource.fromDate != null && resource.toDate != null
    val daysBetween = daysBetween(resource.fromDate, resource.toDate)

    if (isPeriodAdjustment && resource.days != null) {
      if (daysBetween != resource.days) {
        throw ApiValidationException("The number of days provide does not match the period between the from and to dates of the adjustment")
      }
    }
    val prisoner = prisonApiClient.getPrisonerDetail(adjustment.person)
    val persistedLegacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val change = objectToJson(adjustment)
    val sentenceInfo = sentenceInfo(resource)
    adjustment.apply {
      effectiveDays = daysBetween ?: resource.days!!
      days = if (isPeriodAdjustment) null else resource.days
      daysCalculated = daysBetween
      fromDate = resource.fromDate
      toDate = resource.toDate
      source = AdjustmentSource.DPS
      status = ACTIVE
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          sentenceInfo?.sentenceSequence,
          persistedLegacyData.postedDate,
          persistedLegacyData.comment,
          legacyType(resource.adjustmentType, sentenceInfo),
          chargeIds = resource.remand?.chargeId ?: emptyList(),
          caseSequence = resource.taggedBail?.caseSequence,
        ),
      )
      additionalDaysAwarded = additionalDaysAwarded(resource, this)
      unlawfullyAtLarge = unlawfullyAtLarge(resource, this)
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
        prisonId = prisoner.agencyId,
      )
    }
  }

  private fun legacyType(type: AdjustmentType, sentenceInfo: SentenceInfo?): LegacyAdjustmentType? {
    if (sentenceInfo?.recall == true) {
      if (type == REMAND) {
        return LegacyAdjustmentType.RSR
      }
      if (type == TAGGED_BAIL) {
        return LegacyAdjustmentType.RST
      }
    }
    return null
  }

  private fun sentenceInfo(resource: AdjustmentDto): SentenceInfo? {
    if (resource.adjustmentType.isSentenceType()) {
      val sentences = prisonService.getSentencesAndOffences(resource.bookingId)
      return if (resource.remand != null && resource.adjustmentType == REMAND) {
        val matchingSentences =
          sentences.filter { it.offences.any { off -> resource.remand.chargeId.contains(off.offenderChargeId) } }
        if (matchingSentences.isEmpty()) {
          throw ApiValidationException("No matching sentences for charge ids ${resource.remand.chargeId.joinToString()}")
        }
        SentenceInfo(matchingSentences.maxBy { it.sentenceDate })
      } else if (resource.taggedBail != null && resource.adjustmentType == TAGGED_BAIL) {
        val matchingSentences = sentences.filter { it.caseSequence == resource.taggedBail.caseSequence }
        if (matchingSentences.isEmpty()) {
          throw ApiValidationException("No matching sentences for caseSequence ${resource.taggedBail.caseSequence}")
        }
        SentenceInfo(matchingSentences.maxBy { it.sentenceDate })
      } else {
        val matchingSentence = sentences.find { it.sentenceSequence == resource.sentenceSequence }
          ?: throw ApiValidationException("No matching sentences for sentence sequence ${resource.sentenceSequence}")
        SentenceInfo(matchingSentence)
      }
    }
    return null
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val prisoner = prisonApiClient.getPrisonerDetail(adjustment.person)
    val change = objectToJson(adjustment)
    adjustment.apply {
      status = if (this.status == INACTIVE) INACTIVE_WHEN_DELETED else DELETED
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.DPS,
        change = change,
        adjustment = adjustment,
        prisonId = prisoner.agencyId,
      )
    }
  }

  private fun objectToJson(subject: Any): JsonNode {
    return JacksonUtil.toJsonNode(objectMapper.writeValueAsString(subject))
  }

  private fun mapToDto(adjustment: Adjustment): AdjustmentDto {
    val latestHistory = adjustment.adjustmentHistory.last { it.changeType !in listOf(ChangeType.MERGE, ChangeType.RELEASE, ChangeType.ADMISSION) }
    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    val prisonDescription = latestHistory.prisonId?.let { prisonApiClient.getPrison(it).description }
    return AdjustmentDto(
      id = adjustment.id,
      person = adjustment.person,
      effectiveDays = adjustment.effectiveDays,
      fromDate = adjustment.fromDate,
      toDate = adjustment.toDate,
      adjustmentType = adjustment.adjustmentType,
      sentenceSequence = legacyData.sentenceSequence,
      bookingId = legacyData.bookingId,
      additionalDaysAwarded = additionalDaysAwardedToDto(adjustment),
      unlawfullyAtLarge = unlawfullyAtLargeDto(adjustment),
      remand = remandDto(adjustment, legacyData),
      taggedBail = taggedBailDto(adjustment, legacyData),
      lastUpdatedBy = latestHistory.changeByUsername,
      lastUpdatedDate = latestHistory.changeAt,
      createdDate = adjustment.adjustmentHistory.first().changeAt,
      status = adjustment.status,
      prisonId = latestHistory.prisonId,
      prisonName = prisonDescription,
      adjustmentTypeText = adjustment.adjustmentType.text,
      days = adjustment.days ?: daysBetween(adjustment.fromDate, adjustment.toDate) ?: adjustment.effectiveDays,
      adjustmentArithmeticType = adjustment.adjustmentType.arithmeticType,
      source = adjustment.source,
    )
  }

  private fun remandDto(adjustment: Adjustment, legacyData: LegacyData): RemandDto? {
    if (adjustment.adjustmentType === REMAND && legacyData.chargeIds.isNotEmpty()) {
      return RemandDto(legacyData.chargeIds)
    }
    return null
  }

  private fun taggedBailDto(adjustment: Adjustment, legacyData: LegacyData): TaggedBailDto? {
    if (adjustment.adjustmentType === TAGGED_BAIL && legacyData.caseSequence != null) {
      return TaggedBailDto(legacyData.caseSequence)
    }
    return null
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
        adjudicationId = adjustment.additionalDaysAwarded!!.adjudicationCharges.map { it.adjudicationId },
        prospective = adjustment.additionalDaysAwarded!!.prospective,
      )
    }
    return null
  }

  @Transactional
  fun restore(resource: RestoreAdjustmentsDto): List<AdjustmentDto> {
    val adjustments = adjustmentRepository.findAllById(resource.ids).map { mapToDto(it) }
    adjustments.forEach { update(it.id!!, it) }
    return adjustments
  }

  companion object {
    fun daysBetween(from: LocalDate?, to: LocalDate?): Int? =
      from?.let { fromDate -> to?.let { toDate -> (ChronoUnit.DAYS.between(fromDate, toDate) + 1).toInt() } }
  }
}
