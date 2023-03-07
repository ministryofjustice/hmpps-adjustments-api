package uk.gov.justice.digital.hmpps.adjustments.api.respository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment

@Repository
interface AdjustmentRepository : JpaRepository<Adjustment, Long>
