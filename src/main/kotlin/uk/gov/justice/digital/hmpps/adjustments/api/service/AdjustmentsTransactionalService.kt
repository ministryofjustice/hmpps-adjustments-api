package uk.gov.justice.digital.hmpps.adjustments.api.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient
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
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.APPEAL_APPLICANT
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.CUSTODY_ABROAD
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.LAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.REMAND
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.SPECIAL_REMISSION
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.TAGGED_BAIL
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.LawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.entity.SpecialRemission
import uk.gov.justice.digital.hmpps.adjustments.api.entity.TaggedBail
import uk.gov.justice.digital.hmpps.adjustments.api.entity.TimeSpentAsAnAppealApplicant
import uk.gov.justice.digital.hmpps.adjustments.api.entity.TimeSpentInCustodyAbroad
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.error.ApiValidationException
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.LawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.SentenceInfo
import uk.gov.justice.digital.hmpps.adjustments.api.model.SpecialRemissionDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TimeSpentAsAnAppealApplicantDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TimeSpentInCustodyAbroadDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.CourtCase
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
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val remandAndSentencingApiClient: RemandAndSentencingApiClient,
  private val sentenceInfoService: SentenceInfoService,
) {

  fun getCurrentAuthenticationUsername(): String = (SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?)?.principal
    ?: UserContext.getOverrideUsername()

  @Transactional
  fun create(resource: List<AdjustmentDto>): CreateResponseDto = CreateResponseDto(resource.map { create(it) })

  private fun create(resource: AdjustmentDto): UUID {
    val isPeriodAdjustment = resource.fromDate != null && resource.toDate != null
    val daysBetween = daysBetween(resource.fromDate, resource.toDate)

    if (isPeriodAdjustment && resource.days != null) {
      if (daysBetween != resource.days) {
        throw ApiValidationException("The number of days provide does not match the period between the from and to dates of the adjustment")
      }
    }

    validateTaggedBail(resource)

    val sentenceInfo = sentenceInfo(resource)
    val prisoner = prisonerSearchApiClient.findByPrisonerNumber(resource.person)
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
      currentPeriodOfCustody = true,
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          sentenceInfo?.sentenceSequence,
          LocalDate.now(),
          null,
          legacyType(resource.adjustmentType, sentenceInfo),
          chargeIds = getChargeIds(resource),
          caseSequence = resource.taggedBail?.caseSequence,
        ),
      ),
      additionalDaysAwarded = additionalDaysAwarded(resource),
      unlawfullyAtLarge = unlawfullyAtLarge(resource),
      lawfullyAtLarge = lawfullyAtLarge(resource),
      specialRemission = specialRemission(resource),
      taggedBail = taggedBail(resource),
      timeSpentInCustodyAbroad = timeSpentInCustodyAbroad(resource),
      timeSpentAsAnAppealApplicant = timeSpentAsAnAppealApplicant(resource),
      recallId = resource.recallId,
    )
    adjustment.adjustmentHistory = listOf(
      AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.CREATE,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
        prisonId = prisoner.prisonId,
      ),
    )

    return adjustmentRepository.save(adjustment).id
  }

  fun getChargeIds(resource: AdjustmentDto): List<Long> = if (resource.adjustmentType == REMAND && resource.remand != null) {
    resource.remand.chargeId
  } else if (resource.adjustmentType == CUSTODY_ABROAD && resource.timeSpentInCustodyAbroad != null) {
    resource.timeSpentInCustodyAbroad.chargeIds ?: emptyList()
  } else if (resource.adjustmentType == APPEAL_APPLICANT && resource.timeSpentAsAnAppealApplicant != null) {
    resource.timeSpentAsAnAppealApplicant.chargeIds
  } else {
    emptyList()
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

  private fun validateTaggedBail(adjustmentDto: AdjustmentDto) {
    if (adjustmentDto.taggedBail != null && adjustmentDto.taggedBail.courtCaseUuid == null && adjustmentDto.taggedBail.caseSequence == null) {
      throw ApiValidationException("Either courtCaseUuid or caseSequence must be provided for a tagged bail adjustment")
    }
  }

  private fun unlawfullyAtLarge(adjustmentDto: AdjustmentDto, adjustment: Adjustment? = null): UnlawfullyAtLarge? = if (adjustmentDto.adjustmentType == UNLAWFULLY_AT_LARGE && adjustmentDto.unlawfullyAtLarge != null) {
    getUnlawfullyAtLarge(adjustment).apply {
      type = adjustmentDto.unlawfullyAtLarge.type
    }
  } else {
    null
  }

  private fun getUnlawfullyAtLarge(adjustment: Adjustment?) = if (adjustment?.unlawfullyAtLarge != null) {
    adjustment.unlawfullyAtLarge!!
  } else if (adjustment != null) {
    UnlawfullyAtLarge(adjustment = adjustment)
  } else {
    UnlawfullyAtLarge()
  }

  private fun taggedBail(adjustmentDto: AdjustmentDto): TaggedBail? {
    val courtCase: CourtCase?
    if (adjustmentDto.adjustmentType == TAGGED_BAIL && adjustmentDto.taggedBail?.courtCaseUuid != null) {
      try {
        courtCase = remandAndSentencingApiClient.validateCourtCase(adjustmentDto.taggedBail.courtCaseUuid)
      } catch (e: WebClientResponseException) {
        throw ApiValidationException("No court case found with id ${adjustmentDto.taggedBail.courtCaseUuid}")
      }
      if (courtCase != null) {
        return TaggedBail(courtCaseUuid = adjustmentDto.taggedBail.courtCaseUuid)
      }
    }
    return null
  }

  private fun lawfullyAtLarge(adjustmentDto: AdjustmentDto, adjustment: Adjustment? = null): LawfullyAtLarge? = if (adjustmentDto.adjustmentType == LAWFULLY_AT_LARGE && adjustmentDto.lawfullyAtLarge != null) {
    getLawfullyAtLarge(adjustment).apply {
      affectsDates = adjustmentDto.lawfullyAtLarge.affectsDates
    }
  } else {
    null
  }

  private fun getLawfullyAtLarge(adjustment: Adjustment?) = if (adjustment?.lawfullyAtLarge != null) {
    adjustment.lawfullyAtLarge!!
  } else if (adjustment != null) {
    LawfullyAtLarge(adjustment = adjustment)
  } else {
    LawfullyAtLarge()
  }

  private fun specialRemission(adjustmentDto: AdjustmentDto, adjustment: Adjustment? = null): SpecialRemission? = if (adjustmentDto.adjustmentType == SPECIAL_REMISSION && adjustmentDto.specialRemission != null) {
    getSpecialRemission(adjustment).apply {
      type = adjustmentDto.specialRemission.type
    }
  } else {
    null
  }

  private fun getSpecialRemission(adjustment: Adjustment?) = if (adjustment?.specialRemission != null) {
    adjustment.specialRemission!!
  } else if (adjustment != null) {
    SpecialRemission(adjustment = adjustment)
  } else {
    SpecialRemission()
  }

  private fun timeSpentInCustodyAbroad(
    adjustmentDto: AdjustmentDto,
    adjustment: Adjustment? = null,
  ): TimeSpentInCustodyAbroad? = if (adjustmentDto.adjustmentType == CUSTODY_ABROAD && adjustmentDto.timeSpentInCustodyAbroad != null) {
    getTimeSpentInCustodyAbroad(adjustment).apply {
      documentationSource = adjustmentDto.timeSpentInCustodyAbroad.documentationSource
    }
  } else {
    null
  }

  private fun getTimeSpentInCustodyAbroad(adjustment: Adjustment?) = if (adjustment?.timeSpentInCustodyAbroad != null) {
    adjustment.timeSpentInCustodyAbroad!!
  } else if (adjustment != null) {
    TimeSpentInCustodyAbroad(adjustment = adjustment)
  } else {
    TimeSpentInCustodyAbroad()
  }

  private fun timeSpentAsAnAppealApplicant(
    adjustmentDto: AdjustmentDto,
    adjustment: Adjustment? = null,
  ): TimeSpentAsAnAppealApplicant? = if (adjustmentDto.adjustmentType == APPEAL_APPLICANT && adjustmentDto.timeSpentAsAnAppealApplicant != null) {
    getTimeSpentAsAnAppealApplicant(adjustment).apply {
      courtOfAppealReferenceNumber =
        adjustmentDto.timeSpentAsAnAppealApplicant.courtOfAppealReferenceNumber?.uppercase()
    }
  } else {
    null
  }

  private fun getTimeSpentAsAnAppealApplicant(adjustment: Adjustment?) = if (adjustment?.timeSpentAsAnAppealApplicant != null) {
    adjustment.timeSpentAsAnAppealApplicant!!
  } else if (adjustment != null) {
    TimeSpentAsAnAppealApplicant(adjustment = adjustment)
  } else {
    TimeSpentAsAnAppealApplicant()
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

  private fun getAdditionalDaysAwarded(adjustment: Adjustment?) = if (adjustment?.additionalDaysAwarded != null) {
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
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    recallId: UUID?,
  ): List<AdjustmentDto> = adjustmentRepository.findAdjustmentsByPersonWithinSentenceEnvelope(
    person,
    status,
    currentPeriodOfCustody,
    recallId,
  ).map { mapToDto(it) }

  @Transactional
  fun update(adjustmentId: UUID, resource: AdjustmentDto) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status.isDeleted()) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    return update(adjustment, resource)
  }

  private fun update(adjustment: Adjustment, resource: AdjustmentDto) {
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

    validateTaggedBail(resource)

    val prisoner = prisonerSearchApiClient.findByPrisonerNumber(adjustment.person)
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
      currentPeriodOfCustody = true
      legacyData = objectToJson(
        LegacyData(
          resource.bookingId,
          sentenceInfo?.sentenceSequence,
          persistedLegacyData.postedDate,
          persistedLegacyData.comment,
          legacyType(resource.adjustmentType, sentenceInfo),
          chargeIds = getChargeIds(resource),
          caseSequence = resource.taggedBail?.caseSequence,
        ),
      )
      additionalDaysAwarded = additionalDaysAwarded(resource, this)
      unlawfullyAtLarge = unlawfullyAtLarge(resource, this)
      lawfullyAtLarge = lawfullyAtLarge(resource, this)
      specialRemission = specialRemission(resource, this)
      taggedBail = taggedBail(resource)
      timeSpentInCustodyAbroad = timeSpentInCustodyAbroad(resource, this)
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.DPS,
        adjustment = adjustment,
        prisonId = prisoner.prisonId,
      )
      recallId = resource.recallId
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
        getSentenceInfoFromChargeIds(resource.remand.chargeId, sentences)
      } else if (resource.timeSpentInCustodyAbroad != null && resource.adjustmentType == CUSTODY_ABROAD) {
        getSentenceInfoFromChargeIds(resource.timeSpentInCustodyAbroad.chargeIds, sentences)
      } else if (resource.timeSpentAsAnAppealApplicant != null && resource.adjustmentType == APPEAL_APPLICANT) {
        getSentenceInfoFromChargeIds(resource.timeSpentAsAnAppealApplicant.chargeIds, sentences)
      } else if (resource.taggedBail != null && resource.adjustmentType == TAGGED_BAIL) {
        if (resource.taggedBail.caseSequence != null) {
          val matchingSentences = sentences.filter { it.caseSequence == resource.taggedBail.caseSequence }
          if (matchingSentences.isEmpty()) {
            throw ApiValidationException("No matching sentences for caseSequence ${resource.taggedBail.caseSequence}")
          }
          val maxBySentenceDate = matchingSentences.maxBy { it.sentenceDate }
          SentenceInfo(maxBySentenceDate, sentenceInfoService.isRecall(maxBySentenceDate.sentenceCalculationType))
        } else {
          null
        }
      } else {
        val matchingSentence = sentences.find { it.sentenceSequence == resource.sentenceSequence }
          ?: throw ApiValidationException("No matching sentences for sentence sequence ${resource.sentenceSequence}")
        SentenceInfo(matchingSentence, sentenceInfoService.isRecall(matchingSentence.sentenceCalculationType))
      }
    }
    return null
  }

  fun getSentenceInfoFromChargeIds(chargeIds: List<Long>, sentences: List<SentenceAndOffences>): SentenceInfo? {
    val matchingSentences =
      sentences.filter { it.offences.any { off -> chargeIds.contains(off.offenderChargeId) } }
    if (matchingSentences.isEmpty()) {
      throw ApiValidationException("No matching sentences for charge ids ${chargeIds.joinToString()}")
    }
    val activeSentences = matchingSentences.filter { it.sentenceStatus == "A" }
    val sentence = activeSentences.ifEmpty {
      matchingSentences
    }.maxBy { it.sentenceDate }
    return SentenceInfo(
      sentence,
      sentenceInfoService.isRecall(sentence.sentenceCalculationType),
    )
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val prisoner = prisonerSearchApiClient.findByPrisonerNumber(adjustment.person)
    val change = objectToJson(adjustment)
    adjustment.apply {
      status = if (this.status == INACTIVE) INACTIVE_WHEN_DELETED else DELETED
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = getCurrentAuthenticationUsername(),
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.DPS,
        change = change,
        adjustment = adjustment,
        prisonId = prisoner.prisonId,
      )
    }
  }

  private fun objectToJson(subject: Any): JsonNode = JacksonUtil.toJsonNode(objectMapper.writeValueAsString(subject))

  private fun mapToDto(adjustment: Adjustment): AdjustmentDto {
    val latestHistory = adjustment.adjustmentHistory.sortedBy { it.changeAt }.last { it.changeType !in listOf(ChangeType.MERGE, ChangeType.RELEASE, ChangeType.ADMISSION) }
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
      lawfullyAtLarge = lawfullyAtLargeDto(adjustment),
      specialRemission = specialRemissionDto(adjustment),
      remand = remandDto(adjustment, legacyData),
      taggedBail = taggedBailDto(adjustment, legacyData),
      timeSpentInCustodyAbroad = timeSpentInCustodyAbroadDto(adjustment, legacyData),
      timeSpentAsAnAppealApplicant = timeSpentAsAnAppealApplicantDto(adjustment, legacyData),
      lastUpdatedBy = latestHistory.changeByUsername,
      lastUpdatedDate = latestHistory.changeAt,
      createdDate = adjustment.adjustmentHistory.first().changeAt,
      status = adjustment.status,
      prisonId = latestHistory.prisonId,
      prisonName = prisonDescription,
      adjustmentTypeText = adjustment.adjustmentType.text,
      days = getDtoDays(adjustment),
      adjustmentArithmeticType = adjustment.adjustmentType.arithmeticType,
      source = adjustment.source,
      recallId = adjustment.recallId,
    )
  }

  private fun getDtoDays(adjustment: Adjustment): Int {
    if (adjustment.source == AdjustmentSource.NOMIS) {
      return adjustment.effectiveDays
    }
    return adjustment.days ?: daysBetween(adjustment.fromDate, adjustment.toDate) ?: adjustment.effectiveDays
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
    if (adjustment.adjustmentType === TAGGED_BAIL && adjustment.taggedBail?.courtCaseUuid != null) {
      return TaggedBailDto(courtCaseUuid = adjustment.taggedBail?.courtCaseUuid)
    }
    return null
  }

  private fun unlawfullyAtLargeDto(adjustment: Adjustment): UnlawfullyAtLargeDto? = if (adjustment.unlawfullyAtLarge != null) {
    UnlawfullyAtLargeDto(type = adjustment.unlawfullyAtLarge!!.type)
  } else {
    null
  }

  private fun lawfullyAtLargeDto(adjustment: Adjustment): LawfullyAtLargeDto? = if (adjustment.lawfullyAtLarge != null) {
    LawfullyAtLargeDto(affectsDates = adjustment.lawfullyAtLarge!!.affectsDates)
  } else {
    null
  }

  private fun specialRemissionDto(adjustment: Adjustment): SpecialRemissionDto? = if (adjustment.specialRemission != null) {
    SpecialRemissionDto(type = adjustment.specialRemission!!.type)
  } else {
    null
  }

  private fun timeSpentInCustodyAbroadDto(
    adjustment: Adjustment,
    legacyData: LegacyData,
  ): TimeSpentInCustodyAbroadDto? = if (adjustment.timeSpentInCustodyAbroad != null) {
    TimeSpentInCustodyAbroadDto(
      documentationSource = adjustment.timeSpentInCustodyAbroad!!.documentationSource,
      chargeIds = legacyData.chargeIds,
    )
  } else {
    null
  }

  private fun timeSpentAsAnAppealApplicantDto(
    adjustment: Adjustment,
    legacyData: LegacyData,
  ): TimeSpentAsAnAppealApplicantDto? = if (adjustment.timeSpentAsAnAppealApplicant != null) {
    TimeSpentAsAnAppealApplicantDto(
      courtOfAppealReferenceNumber = adjustment.timeSpentAsAnAppealApplicant!!.courtOfAppealReferenceNumber,
      chargeIds = legacyData.chargeIds,
    )
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
    val adjustments = adjustmentRepository.findAllById(resource.ids).map { it to mapToDto(it) }
    adjustments.forEach { (adjustment, dto) -> update(adjustment, dto) }
    return adjustments.map { (_, dto) -> dto }
  }

  companion object {
    fun daysBetween(from: LocalDate?, to: LocalDate?): Int? = from?.let { fromDate -> to?.let { toDate -> (ChronoUnit.DAYS.between(fromDate, toDate) + 1).toInt() } }
  }
}
