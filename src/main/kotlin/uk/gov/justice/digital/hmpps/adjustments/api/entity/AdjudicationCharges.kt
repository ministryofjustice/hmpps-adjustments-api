package uk.gov.justice.digital.hmpps.adjustments.api.entity

import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull

@Embeddable
data class AdjudicationCharges(
  @NotNull
  var adjudicationId: Long = -1,
)
