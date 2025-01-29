package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of the time spent as an appeal applicant adjustment")
data class TimeSpentAsAnAppealApplicantDto(
  @Schema(description = "The court of appeal reference number for the time spent as an appeal applicant adjustment")
  val courtOfAppealReferenceNumber: String? = null,
)
