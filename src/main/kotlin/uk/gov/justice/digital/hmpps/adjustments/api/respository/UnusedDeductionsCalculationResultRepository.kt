package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnusedDeductionsCalculationResult
import java.util.UUID

@Repository
interface UnusedDeductionsCalculationResultRepository : JpaRepository<UnusedDeductionsCalculationResult, UUID> {
  fun findFirstByPerson(person: String): UnusedDeductionsCalculationResult?
}
