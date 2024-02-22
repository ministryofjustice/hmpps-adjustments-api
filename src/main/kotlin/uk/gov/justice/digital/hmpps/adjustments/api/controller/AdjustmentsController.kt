package uk.gov.justice.digital.hmpps.adjustments.api.controller

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.EditableAdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdditionalDaysAwardedService
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsEventService
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.adjustments.api.service.ValidationService
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/adjustments", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "adjustment-controller", description = "CRUD operations for adjustments.")
class AdjustmentsController(
  val adjustmentsService: AdjustmentsService,
  val eventService: AdjustmentsEventService,
  val validationService: ValidationService,
  val additionalDaysAwardedService: AdditionalDaysAwardedService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER') and hasRole('RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create adjustments",
    description = "Create adjustment.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Adjustments created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun create(@RequestBody adjustments: List<EditableAdjustmentDto>): CreateResponseDto {
    return adjustmentsService.create(adjustments).also {
      eventService.create(it.adjustmentIds, adjustments[0].person, AdjustmentSource.DPS, adjustments[0].adjustmentType)
    }
  }

  @GetMapping("", params = ["person"])
  @Operation(
    summary = "Get current adjustments by person",
    description = "Get current adjustments for a given person.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("(hasRole('ADJUSTMENTS_MAINTAINER') and hasRole('RELEASE_DATES_CALCULATOR')) or hasRole('VIEW_SENTENCE_ADJUSTMENTS')")
  fun findByPerson(
    @Parameter(required = true, description = "The noms ID of the person")
    @RequestParam("person")
    person: String,
    @Parameter(required = false, description = "The status of adjustments. Defaults to ACTIVE")
    @RequestParam("status")
    status: AdjustmentStatus?,
    @Parameter(
      required = false,
      description = "The earliest sentence date to filter adjustments by. Defaults to earliest active sentence date",
    )
    @RequestParam("sentenceEnvelopeDate")
    sentenceEnvelopeDate: LocalDate?,
  ): List<AdjustmentDto> {
    return adjustmentsService.findCurrentAdjustments(person, status ?: AdjustmentStatus.ACTIVE, sentenceEnvelopeDate)
  }

  @GetMapping("/{adjustmentId}")
  @Operation(
    summary = "Get an adjustments",
    description = "Get details of an adjustment",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("(hasRole('ADJUSTMENTS_MAINTAINER') and hasRole('RELEASE_DATES_CALCULATOR')) or hasRole('VIEW_SENTENCE_ADJUSTMENTS')")
  fun get(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
  ): AdjustmentDto {
    return adjustmentsService.get(adjustmentId)
  }

  @PutMapping("/{adjustmentId}")
  @Operation(
    summary = "Update an adjustments",
    description = "Update an adjustment.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment update"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER')")
  fun update(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
    @RequestBody adjustment: EditableAdjustmentDto,
  ) {
    adjustmentsService.update(adjustmentId, adjustment).also {
      eventService.update(adjustmentId, adjustment.person, AdjustmentSource.DPS, adjustment.adjustmentType)
    }
  }

  @PostMapping("/restore")
  @Operation(
    summary = "Restore a deleted adjustment",
    description = "Restore a deleted adjustment",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment restored"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER')")
  fun restore(
    @Parameter(required = true, description = "The adjustment UUID")
    @RequestBody
    adjustments: RestoreAdjustmentsDto,
  ) {
    adjustmentsService.restore(adjustments).also {
      eventService.create(adjustments.ids, it[0].person, AdjustmentSource.DPS, it[0].adjustmentType)
    }
  }

  @PostMapping("/{adjustmentId}/effective-days")
  @Operation(
    summary = "Update the effective calculable days for and adjustment",
    description = "Update an adjustment's effective days.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment update"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER')")
  fun updateEffectiveDays(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
    @RequestBody adjustment: AdjustmentEffectiveDaysDto,
  ) {
    adjustmentsService.updateEffectiveDays(adjustmentId, adjustment).also {
      eventService.updateEffectiveDays(adjustmentId, adjustment.person, AdjustmentSource.DPS)
    }
  }

  @DeleteMapping("/{adjustmentId}")
  @Operation(
    summary = "Delete an adjustments",
    description = "Delete an adjustment.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment deleted"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER') and hasRole('RELEASE_DATES_CALCULATOR')")
  fun delete(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
  ) {
    adjustmentsService.get(adjustmentId).also {
      adjustmentsService.delete(adjustmentId)
      eventService.delete(adjustmentId, it.person, AdjustmentSource.DPS, it.adjustmentType)
    }
  }

  @PostMapping("/validate")
  @Operation(
    summary = "Validate an adjustments",
    description = "Validate an adjustment.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment validation returned"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  @PreAuthorize("hasRole('ADJUSTMENTS_MAINTAINER') and hasRole('RELEASE_DATES_CALCULATOR')")
  fun validate(@RequestBody adjustment: EditableAdjustmentDto): List<ValidationMessage> {
    return validationService.validate(adjustment)
  }

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
    return additionalDaysAwardedService.determineAdaIntercept(person)
  }
}
