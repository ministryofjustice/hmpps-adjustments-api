package uk.gov.justice.digital.hmpps.adjustments.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

data class CreateResponseDto(
  val adjustmentId: UUID,
  @JsonIgnore
  val additionalEvent: AdditionalEvent?,
)
