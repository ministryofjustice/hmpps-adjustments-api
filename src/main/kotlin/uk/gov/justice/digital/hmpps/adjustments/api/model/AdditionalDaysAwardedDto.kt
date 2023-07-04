package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of an additional days awarded (ADA) adjustment")
data class AdditionalDaysAwardedDto(
  @Schema(description = "The id of the adjudication that resulted in the ADA")
  val adjudicationId: String,
  @Schema(description = "Is the ADA consecutive or concurrent")
  val consecutive: Boolean,
)
