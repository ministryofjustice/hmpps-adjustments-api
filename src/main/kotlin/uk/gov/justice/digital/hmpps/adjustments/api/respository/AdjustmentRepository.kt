package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
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

  fun findByPersonAndStatus(
    person: String,
    status: AdjustmentStatus,
  ): List<Adjustment>

  fun findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(person: String, adjustmentType: AdjustmentType, status: AdjustmentStatus = ACTIVE, currentPeriodOfCustody: Boolean = true): List<Adjustment>
}
