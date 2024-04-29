package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The DTO representing the PADAs rejected")
data class ProspectiveAdaRejectionDto(
  @Schema(description = "The NOMIS ID of the person this pada is rejected applies to")
  val person: String,
  @Schema(description = "The number of days that were rejected")
  val days: Int,
  @Schema(description = "The date of the charges proved that were rejected")
  val dateChargeProved: LocalDate,
)
