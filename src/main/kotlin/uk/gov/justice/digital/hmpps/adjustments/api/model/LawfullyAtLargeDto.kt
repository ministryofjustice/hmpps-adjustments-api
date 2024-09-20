package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.LawfullyAtLargeAffectsDates

@Schema(description = "The details of a LAL adjustment")
data class LawfullyAtLargeDto(
  @Schema(description = "The type of LAL")
  val affectsDates: LawfullyAtLargeAffectsDates? = null,
)
