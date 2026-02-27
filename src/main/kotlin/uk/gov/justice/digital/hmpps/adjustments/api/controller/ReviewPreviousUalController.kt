package uk.gov.justice.digital.hmpps.adjustments.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeReviewRequest
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsDomainEventService
import uk.gov.justice.digital.hmpps.adjustments.api.service.ReviewPreviousUalService

@RestController
@RequestMapping("/adjustments/person/{person}/review-previous-ual", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Previous UAL",
  description = "Endpoints for reviewing UAL from previous periods of custody and potentially carrying it forward into the current period.",
)
class ReviewPreviousUalController(
  private val reviewPreviousUALService: ReviewPreviousUalService,
  private val adjustmentsDomainEventService: AdjustmentsDomainEventService,
) {

  @GetMapping("")
  @Operation(
    summary = "Get UAL from previous periods of custody requiring review",
    description = "Get any UAL from a previous period of custody that might still be relevant to the current period and requires review",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustments found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW', 'ADJUSTMENTS__ADJUSTMENTS_RO')")
  fun findPreviousUalToReview(
    @PathVariable @Parameter(required = true, description = "The noms ID of the person")
    person: String,
  ): List<PreviousUnlawfullyAtLargeAdjustmentForReview> = reviewPreviousUALService.findPreviousUalToReview(person)

  @PutMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  @Operation(
    summary = "Confirm or reject previous UAL",
    description = "Confirm or reject previous UAL. Once confirmed or rejected it will not be shown again. Confirming UAL create new adjustments for the current period of custody.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "202", description = "Adjustments created or marked as reviewed and rejected"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun confirmPreviousUal(
    @PathVariable @Parameter(required = true, description = "The noms ID of the person")
    person: String,
    @RequestBody request: PreviousUnlawfullyAtLargeReviewRequest,
  ) {
    reviewPreviousUALService.submitPreviousUalReview(person, request).forEach {
      adjustmentsDomainEventService.raiseAdjustmentEvent(it)
    }
  }
}
