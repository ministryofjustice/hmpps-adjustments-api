package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import java.time.LocalDate
import java.util.UUID

@Repository
interface AdjustmentRepository : JpaRepository<Adjustment, UUID> {

  @Query(
    "SELECT a FROM Adjustment a" +
      " WHERE a.person = :person" +
      " AND a.status IN :status" +
      " AND a.currentPeriodOfCustody = :currentPeriodOfCustody" +
      " AND (:recallId IS NULL OR a.recallId = :recallId)",
  )
  fun findAdjustmentsByPersonWithinSentenceEnvelope(
    person: String,
    status: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
    recallId: UUID?,
  ): List<Adjustment>

  fun findByPerson(person: String): List<Adjustment>

  fun findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(person: String, adjustmentType: AdjustmentType, status: AdjustmentStatus = ACTIVE, currentPeriodOfCustody: Boolean = true): List<Adjustment>

  @Query(
    """
      SELECT a FROM Adjustment a
      WHERE a.person = :person 
      AND a.status IN :status 
      AND a.currentPeriodOfCustody = false
      AND a.adjustmentType = :adjustmentType
      AND a.toDate IS NOT NULL AND a.toDate > :startOfSentenceEnvelope
      AND a.id NOT IN (SELECT r.adjustmentId FROM ReviewPreviousUalResult r WHERE r.person = :person)
      """,
  )
  fun findUnreviewedPreviousUALOverlappingSentenceDate(
    person: String,
    startOfSentenceEnvelope: LocalDate,
    status: List<AdjustmentStatus> = listOf(ACTIVE, INACTIVE),
    adjustmentType: AdjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
  ): List<Adjustment>


  fun findByPersonAndAdjustmentTypeAndStatusInAndCurrentPeriodOfCustody(
    person: String,
    adjustmentType: AdjustmentType,
    statuses: List<AdjustmentStatus>,
    currentPeriodOfCustody: Boolean,
  ): List<Adjustment>
}
