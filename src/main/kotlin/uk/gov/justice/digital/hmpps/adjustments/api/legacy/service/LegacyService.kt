package uk.gov.justice.digital.hmpps.adjustments.api.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
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
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentUpdatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonersearchapi.Prisoner
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LegacyService(
  private val adjustmentRepository: AdjustmentRepository,
  private val objectMapper: ObjectMapper,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {

  fun getBookingIdFromLegacyData(legacyData: JsonNode?): String {
    val legacyData = objectMapper.convertValue(legacyData, LegacyData::class.java)
    return legacyData.bookingId.toString()
  }

  @Transactional
  fun create(resource: LegacyAdjustment, migration: Boolean): LegacyAdjustmentCreatedResponse {
    val prisonId = if (migration) null else resource.agencyId
    var adjustment = Adjustment(
      person = resource.offenderNo,
      effectiveDays = resource.adjustmentDays,
      fromDate = resource.adjustmentFromDate,
      toDate = resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1),
      source = AdjustmentSource.NOMIS,
      adjustmentType = transform(resource.adjustmentType),
      status = if (resource.active) ACTIVE else INACTIVE,
      currentPeriodOfCustody = resource.currentTerm,
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, migration, adjustmentActive = resource.active)),
      adjustmentHistory = listOf(
        AdjustmentHistory(
          changeByUsername = "NOMIS",
          changeType = ChangeType.CREATE,
          changeSource = AdjustmentSource.NOMIS,
          prisonId = prisonId,
        ),
      ),
    )

    adjustment = adjustmentRepository.save(adjustment)

    return LegacyAdjustmentCreatedResponse(adjustment.id)
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
      active = legacyData.adjustmentActive,
      bookingReleased = false,
      comment = legacyData.comment,
      agencyId = null,
      currentTerm = adjustment.currentPeriodOfCustody,
    )
  }

  @Transactional
  fun update(adjustmentId: UUID, resource: LegacyAdjustment): LegacyAdjustmentUpdatedResponse {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status.isDeleted()) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    val prisonId = resource.agencyId
    val change = objectToJson(adjustment)
    val isChangeToDays = adjustment.effectiveDays != resource.adjustmentDays || adjustment.fromDate != resource.adjustmentFromDate

    adjustment.apply {
      effectiveDays = resource.adjustmentDays
      fromDate = resource.adjustmentFromDate
      toDate = if (isChangeToDays) resource.adjustmentFromDate?.plusDays(resource.adjustmentDays.toLong() - 1) else this.toDate
      days = if (this.days != null && isChangeToDays) resource.adjustmentDays else this.days
      daysCalculated = if (this.daysCalculated != null && isChangeToDays) resource.adjustmentDays else this.daysCalculated
      source = AdjustmentSource.NOMIS
      status = if (resource.active) ACTIVE else INACTIVE
      currentPeriodOfCustody = resource.currentTerm
      legacyData = objectToJson(LegacyData(resource.bookingId, resource.sentenceSequence, resource.adjustmentDate, resource.comment, resource.adjustmentType, false, adjustmentActive = resource.active))
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

    return LegacyAdjustmentUpdatedResponse(isChangeToDays)
  }

  @Transactional
  fun updateAllAdjustmentsToHaveEffectiveDaysAsDpsDays(person: String, bookingId: Long, apiPrisonId: String? = null) {
    val prisonId = apiPrisonId ?: prisonerSearchApiClient.findByPrisonerNumber(person).prisonId
    val adjustments = adjustmentRepository.findByPersonAndStatus(person, ACTIVE)
    adjustments
      .filter {
        val legacyData = objectMapper.convertValue(it.legacyData, LegacyData::class.java)
        legacyData.bookingId == bookingId
      }
      .forEach {
        val dpsDays = it.days ?: it.daysCalculated
        if (dpsDays != null && dpsDays != it.effectiveDays) {
          val change = objectToJson(it)
          it.apply {
            toDate = it.fromDate?.plusDays(it.effectiveDays.toLong() - 1)
            days = if (this.days != null) it.effectiveDays else this.days
            daysCalculated = if (this.daysCalculated != null) it.effectiveDays else this.daysCalculated
            source = AdjustmentSource.NOMIS
            adjustmentHistory += AdjustmentHistory(
              changeByUsername = "NOMIS",
              changeType = ChangeType.RESET_DAYS,
              change = change,
              changeSource = AdjustmentSource.NOMIS,
              adjustment = it,
              prisonId = prisonId,
            )
          }
        }
      }
  }

  @Transactional
  fun delete(adjustmentId: UUID) {
    val adjustment = adjustmentRepository.findById(adjustmentId)
      .map { if (it.status.isDeleted()) null else it }
      .orElseThrow {
        EntityNotFoundException("No adjustment found with id $adjustmentId")
      }!!
    val prisonId = prisonerSearchApiClient.findByPrisonerNumber(adjustment.person).prisonId
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
      AdjustmentType.CUSTODY_ABROAD -> LegacyAdjustmentType.TCA
      AdjustmentType.APPEAL_APPLICANT -> LegacyAdjustmentType.TSA
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
      LegacyAdjustmentType.TCA -> AdjustmentType.CUSTODY_ABROAD
      LegacyAdjustmentType.TSA -> AdjustmentType.APPEAL_APPLICANT
    }
  }

  @Transactional
  fun setAdmission(prisoner: Prisoner) {
    val adjustments = adjustmentRepository.findByPerson(prisoner.prisonerNumber)

    adjustments.forEach {
      val persistedLegacyData = objectMapper.convertValue(it.legacyData, LegacyData::class.java)
      val isCurrentBooking = persistedLegacyData.bookingId == prisoner.bookingId
      if (it.currentPeriodOfCustody != isCurrentBooking) {
        it.apply {
          currentPeriodOfCustody = isCurrentBooking
        }
        it.adjustmentHistory += AdjustmentHistory(
          changeByUsername = "NOMIS",
          changeType = ChangeType.ADMISSION,
          changeSource = AdjustmentSource.NOMIS,
          adjustment = it,
          prisonId = prisoner.prisonId,
        )
      }
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

  @Transactional
  fun fixCurrentTermForPrisoner(prisonerId: String) {
    val currentBookingId = prisonerSearchApiClient.findByPrisonerNumber(prisonerId).bookingId
    val adjustments = adjustmentRepository.findByPerson(prisonerId)
    adjustments.forEach { adjustment ->
      val adjBookingId = getBookingIdFromLegacyData(adjustment.legacyData)
      if (adjBookingId == currentBookingId.toString()) {
        adjustment.currentPeriodOfCustody = true
      }
    }
  }

  @Transactional
  fun moveBooking(bookingId: String, movedFromNomsNumber: String, movedToNomsNumber: String) {
    // Find all adjustments for the old prisoner
    val adjustmentsForMovedFrom = adjustmentRepository.findByPerson(movedFromNomsNumber)

    // Filter adjustments by bookingId extracted from legacyData
    val filteredAdjustments = adjustmentsForMovedFrom.filter { this.getBookingIdFromLegacyData(it.legacyData) == bookingId }

    // Update each adjustment to the new prisoner
    filteredAdjustments.forEach { adjustment ->
      adjustment.apply {
        person = movedToNomsNumber
        adjustmentHistory += AdjustmentHistory(
          changeByUsername = "NOMIS",
          changeType = ChangeType.BOOKING_MOVE,
          changeSource = AdjustmentSource.NOMIS,
          adjustment = this,
        )
      }
    }
  }
}
