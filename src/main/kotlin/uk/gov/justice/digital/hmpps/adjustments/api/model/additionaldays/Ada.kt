package uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays

import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus
import java.time.LocalDate

data class Ada(
  val dateChargeProved: LocalDate,
  val chargeNumber: Long,
  val toBeServed: String? = null,
  val heardAt: String? = null,
  val status: ChargeStatus,
  val days: Int,
  val sequence: Long? = null,
  val consecutiveToSequence: Long? = null,
)
