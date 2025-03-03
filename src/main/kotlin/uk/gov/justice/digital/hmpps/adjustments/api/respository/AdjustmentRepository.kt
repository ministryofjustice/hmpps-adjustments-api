package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.REMAND
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.TAGGED_BAIL
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNUSED_DEDUCTIONS
import java.time.LocalDate
import java.util.UUID

@Repository
interface AdjustmentRepository : JpaRepository<Adjustment, UUID> {

  @Query(
    "SELECT a FROM Adjustment a" +
      " LEFT JOIN a.additionalDaysAwarded ada" +
      " WHERE a.person = :person" +
      " AND a.status IN :status" +
      " AND a.currentPeriodOfCustody = :currentPeriodOfCustody" +
      " AND (:recallId IS NULL OR a.recallId = :recallId)" +
      " AND (" +
      " :sentenceEnvelopeFilter IS NULL" +
      " OR a.fromDate IS NULL" +
      " OR a.fromDate >= :sentenceEnvelopeFilter" +
      " OR a.adjustmentType IN (:adjustmentTypes)" +
      " OR ada.prospective" +
      " OR (a.adjustmentType = 'ADDITIONAL_DAYS_AWARDED' AND ada IS NULL)" +
      ")",
  )
  fun findAdjustmentsByPersonWithinSentenceEnvelope(
    person: String,
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    sentenceEnvelopeFilter: LocalDate?,
    recallId: UUID?,
    adjustmentTypes: List<AdjustmentType>? = listOf(REMAND, TAGGED_BAIL, UNUSED_DEDUCTIONS),
  ): List<Adjustment>

  fun findByPerson(person: String): List<Adjustment>

  fun findByPersonAndStatus(
    person: String,
    status: AdjustmentStatus,
  ): List<Adjustment>

  fun findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(person: String, adjustmentType: AdjustmentType, status: AdjustmentStatus = ACTIVE, currentPeriodOfCustody: Boolean = true): List<Adjustment>
}
