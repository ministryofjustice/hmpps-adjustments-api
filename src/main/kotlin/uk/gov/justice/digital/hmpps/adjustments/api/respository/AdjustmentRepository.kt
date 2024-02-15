package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
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
      " AND a.status = :status" +
      " AND (" +
      " a.fromDate IS NULL" +
      " OR a.fromDate >= :fromDate" +
      " OR a.adjustmentType IN (:adjustmentTypes)" +
      " OR ada.prospective" +
      ")",
  )
  fun findAdjustmentsByPersonWithinSentenceEnvelope(
    person: String,
    fromDate: LocalDate,
    status: AdjustmentStatus,
    adjustmentTypes: List<AdjustmentType>? = listOf(REMAND, TAGGED_BAIL, UNUSED_DEDUCTIONS),
  ): List<Adjustment>

  fun findByPerson(person: String): List<Adjustment>

  fun findByPersonAndStatus(
    person: String,
    status: AdjustmentStatus,
  ): List<Adjustment>
}
