package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.enums.AdaStatus
import uk.gov.justice.digital.hmpps.adjustments.api.service.Ada
import java.time.LocalDate
import java.util.UUID

data class AdasByDateCharged(
  val dateChargeProved: LocalDate,
  val charges: MutableList<Ada>,
  val total: Int? = null,
  val status: AdaStatus? = null,
  val adjustmentId: UUID? = null,
)
