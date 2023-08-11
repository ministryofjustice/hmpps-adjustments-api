package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.REMAND
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.TAGGED_BAIL
import java.time.LocalDate
import java.util.UUID

@Repository
interface AdjustmentRepository : JpaRepository<Adjustment, UUID> {
  @Query(
    "SELECT a FROM Adjustment a WHERE a.person = :person AND a.deleted = false " +
      "AND (a.fromDate IS NULL  OR a.fromDate >= :fromDate OR a.adjustmentType IN (:adjustmentTypes))",
  )
  fun findCurrentAdjustmentsByPerson(
    person: String,
    fromDate: LocalDate,
    adjustmentTypes: List<AdjustmentType>? = listOf(REMAND, TAGGED_BAIL),
  ): List<Adjustment>
}
