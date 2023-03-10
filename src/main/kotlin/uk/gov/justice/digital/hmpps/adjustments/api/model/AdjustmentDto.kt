package uk.gov.justice.digital.hmpps.adjustments.api.model

import java.util.UUID

data class AdjustmentDto(
  val id: UUID,
  val adjustment: AdjustmentDetailsDto
)
