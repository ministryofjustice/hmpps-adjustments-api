package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.TimeSpentInCustodyAbroadDocumentationSource

@Schema(description = "The details of the time spent in custody abroad adjustment")
data class TimeSpentInCustodyAbroadDto(
  @Schema(description = "The source document for the time spent in custody abroad information")
  val documentationSource: TimeSpentInCustodyAbroadDocumentationSource? = null,
  @Schema(description = "The id of the charges for this time spent in custody abroad adjustment")
  val chargeIds: List<Long>,
)
