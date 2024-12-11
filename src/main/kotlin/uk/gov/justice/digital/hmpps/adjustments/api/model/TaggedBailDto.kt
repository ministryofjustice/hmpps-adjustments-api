package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "The details of the tagged bail adjustment")
data class TaggedBailDto(
  @Schema(description = "The case sequence number this tagged-bail was associated with")
  val caseSequence: Int? = null,
  @Schema(description = "The dps court case id this tagged-bail was associated with")
  val courtCaseUuid: UUID? = null,
)
