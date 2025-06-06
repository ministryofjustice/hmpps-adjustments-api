package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.service.LegacyService
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsDomainEventService
import java.util.UUID

@RestController
@RequestMapping("/legacy/adjustments", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [LegacyController.LEGACY_CONTENT_TYPE])
@Tag(name = "legacy-controller", description = "CRUD operations for syncing data from NOMIS into adjustments api database.")
class LegacyController(
  val legacyService: LegacyService,
  val eventService: AdjustmentsDomainEventService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an adjustments",
    description = "Synchronise a creation from NOMIS into adjustments API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Adjustment created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  @PreAuthorize("hasRole('SENTENCE_ADJUSTMENTS_SYNCHRONISATION')")
  fun create(@RequestBody adjustment: LegacyAdjustment): LegacyAdjustmentCreatedResponse = legacyService.create(adjustment, migration = false).also {
    eventService.create(listOf(it.adjustmentId), adjustment.offenderNo, AdjustmentSource.NOMIS)
  }

  @PostMapping("/migration")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an adjustment from the migration job",
    description = "Synchronise a creation from NOMIS into adjustments API. This endpoint is used for initial migration " +
      "of data from NOMIS without raising any events.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Adjustment created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  @PreAuthorize("hasRole('SENTENCE_ADJUSTMENTS_SYNCHRONISATION')")
  fun migration(@RequestBody adjustment: LegacyAdjustment): LegacyAdjustmentCreatedResponse = legacyService.create(adjustment, migration = true)

  @GetMapping("/{adjustmentId}")
  @Operation(
    summary = "Get an adjustments",
    description = "Get details of an adjustment in the NOMIS system format.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasAnyRole('SENTENCE_ADJUSTMENTS_SYNCHRONISATION', 'ADJUSTMENTS__ADJUSTMENTS_RO')")
  fun get(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
  ): LegacyAdjustment = legacyService.get(adjustmentId)

  @PutMapping("/{adjustmentId}")
  @Operation(
    summary = "Update an adjustments",
    description = "Synchronise an update from NOMIS into adjustments API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment update"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('SENTENCE_ADJUSTMENTS_SYNCHRONISATION')")
  fun update(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
    @RequestBody adjustment: LegacyAdjustment,
  ) {
    legacyService.update(adjustmentId, adjustment).also {
      eventService.update(adjustmentId, adjustment.offenderNo, AdjustmentSource.NOMIS)
    }
  }

  @DeleteMapping("/{adjustmentId}")
  @Operation(
    summary = "Delete an adjustments",
    description = "Synchronise a deletion from NOMIS into adjustments API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment deleted"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('SENTENCE_ADJUSTMENTS_SYNCHRONISATION')")
  fun delete(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
  ) {
    legacyService.get(adjustmentId).also {
      legacyService.delete(adjustmentId)
      eventService.delete(adjustmentId, it.offenderNo, AdjustmentSource.NOMIS)
    }
  }

  companion object {
    const val LEGACY_CONTENT_TYPE = "application/vnd.nomis-offence+json"
  }
}
