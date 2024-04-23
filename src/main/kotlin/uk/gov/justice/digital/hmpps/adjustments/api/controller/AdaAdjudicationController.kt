package uk.gov.justice.digital.hmpps.adjustments.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdditionalDaysAwardedService

@RestController
@RequestMapping("/adjustments/additional-days", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ada-adjudication-controller", description = "Read operations for additional days logic")
class AdaAdjudicationController(
  val additionalDaysAwardedService: AdditionalDaysAwardedService,
) {

  @GetMapping("/{person}/intercept")
  @Operation(
    summary = "Determine if there needs to be an adjustment-interception for this person",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Intercept decision returned"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER')")
  fun determineAdaIntercept(
    @Parameter(required = true, example = "AA1256A", description = "The noms ID of the person")
    @PathVariable("person")
    person: String,
  ): AdaIntercept {
    return additionalDaysAwardedService.getAdaAdjudicationDetails(person).intercept
  }

  @GetMapping("/{person}/adjudication-details")
  @Operation(
    summary = "Get all details of adjudications and associated adjustments",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Details of adjudications and adjustments returned"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER')")
  fun getAdaAdjudicationDetails(
    @Parameter(required = true, example = "AA1256A", description = "The noms ID of the person")
    @PathVariable("person")
    person: String,
    @Parameter(required = true, example = "2022-01-10,2022-02-11", description = "The dates of selected prospective adas")
    @PathVariable("selectedProspectiveAdaDates")
    selectedProspectiveAdaDates: List<String>,
  ): AdaAdjudicationDetails {
    return additionalDaysAwardedService.getAdaAdjudicationDetails(person, selectedProspectiveAdaDates)
  }
}
