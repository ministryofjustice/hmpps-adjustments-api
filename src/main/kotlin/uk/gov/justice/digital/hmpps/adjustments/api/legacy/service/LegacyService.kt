package uk.gov.justice.digital.hmpps.adjustments.api.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.SystemPrisonApiClient
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
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonerDetails
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LegacyService(
  private val adjustmentRepository: AdjustmentRepository,
  private val objectMapper: ObjectMapper,
  private val systemPrisonApiClient: SystemPrisonApiClient,
) {

  @Transactional
  fun create(resource: LegacyAdjustment, migration: Boolean): LegacyAdjustmentCreatedResponse {
    val prisonId = if (migration) null else systemPrisonApiClient.getPrisonerDetail(resource.offenderNo).agencyId
    val adjustment = Adjustment(
      person = resource.offenderNo,
      effectiveDays = resource.adjustmentDays,
      fromDate = resource.adjustmentFromDate,
      toDate = resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1),
      source = AdjustmentSource.NOMIS,
      adjustmentType = transform(resource.adjustmentType),
      status = if (resource.active && !resource.bookingReleased) ACTIVE else INACTIVE,
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, migration, adjustmentActive = resource.active, bookingActive = !resource.bookingReleased)),
      adjustmentHistory = listOf(
        AdjustmentHistory(
          changeByUsername = "NOMIS",
          changeType = ChangeType.CREATE,
          changeSource = AdjustmentSource.NOMIS,
          prisonId = prisonId,
        ),
      ),
    )

    return LegacyAdjustmentCreatedResponse(adjustmentRepository.save(adjustment).id)
  }

  fun get(adjustmentId: UUID): LegacyAdjustment {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status.isDeleted()) null else it }
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
      active = legacyData.adjustmentActive && !shouldSetAdjustmentToInactiveBecauseOfUnusedDeductions(adjustment),
      bookingReleased = !legacyData.bookingActive,
      comment = legacyData.comment,
    )
  }

  /*
   * If the unused deductions' calculation results in this adjustment being ENTIRELY unused. It will then have no bearing
   * on a release date calculation. Usually the users would then not create this adjustment in NOMIS. However, we record
   * all deductions, therefore make this adjustment inactive in NOMIS.
   */
  private fun shouldSetAdjustmentToInactiveBecauseOfUnusedDeductions(adjustment: Adjustment): Boolean {
    val dpsDays = (adjustment.days ?: adjustment.daysCalculated)
    val adjustmentHasDifferentEffectiveDays = dpsDays != null && dpsDays != adjustment.effectiveDays
    return adjustmentHasDifferentEffectiveDays && adjustment.effectiveDays == 0
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: LegacyAdjustment) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }
    val prisonId = systemPrisonApiClient.getPrisonerDetail(resource.offenderNo).agencyId
    val change = objectToJson(adjustment)
    adjustment.apply {
      effectiveDays = resource.adjustmentDays
      fromDate = resource.adjustmentFromDate
      toDate = if (adjustmentHasDPSUnusedDeductions(this)) this.toDate else resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1)
      source = AdjustmentSource.NOMIS
      status = if (resource.active && !resource.bookingReleased) ACTIVE else INACTIVE
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, false, adjustmentActive = resource.active, bookingActive = !resource.bookingReleased))
      adjustmentType = transform(resource.adjustmentType)
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.UPDATE,
        change = change,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = adjustment,
        prisonId = prisonId,
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
    val prisonId = systemPrisonApiClient.getPrisonerDetail(adjustment.person).agencyId
    val change = objectToJson(adjustment)
    adjustment.apply {
      status = if (this.status == INACTIVE) INACTIVE_WHEN_DELETED else DELETED
      adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.DELETE,
        changeSource = AdjustmentSource.NOMIS,
        change = change,
        adjustment = adjustment,
        prisonId = prisonId,
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

  @Transactional
  fun setReleased(prisoner: PrisonerDetails) {
    val adjustments = adjustmentRepository.findByPerson(prisoner.offenderNo)

    adjustments.forEach {
      val persistedLegacyData = objectMapper.convertValue(it.legacyData, LegacyData::class.java).copy(
        bookingActive = false,
      )
      if (persistedLegacyData.bookingId == prisoner.bookingId) {
        it.apply {
          status = if (it.status.isDeleted()) it.status else INACTIVE
          legacyData = objectToJson(persistedLegacyData)
        }
      }

      it.adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.RELEASE,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = it,
        prisonId = prisoner.agencyId,
      )
    }
  }

  @Transactional
  fun setAdmission(prisoner: PrisonerDetails) {
    val adjustments = adjustmentRepository.findByPerson(prisoner.offenderNo)

    adjustments.forEach {
      val persistedLegacyData = objectMapper.convertValue(it.legacyData, LegacyData::class.java).copy(
        bookingActive = true,
      )
      if (persistedLegacyData.bookingId == prisoner.bookingId) {
        it.apply {
          status = if (it.status.isDeleted()) it.status else if (persistedLegacyData.adjustmentActive) ACTIVE else INACTIVE
          legacyData = objectToJson(persistedLegacyData)
        }
      }

      it.adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.ADMISSION,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = it,
        prisonId = prisoner.agencyId,
      )
    }
  }

  @Transactional
  fun prisonerMerged(nomsNumber: String, removedNomsNumber: String) {
    val adjustments = adjustmentRepository.findByPerson(removedNomsNumber)

    adjustments.forEach {
      it.person = nomsNumber
      it.adjustmentHistory += AdjustmentHistory(
        changeByUsername = "NOMIS",
        changeType = ChangeType.MERGE,
        changeSource = AdjustmentSource.NOMIS,
        adjustment = it,
      )
    }
  }
}
