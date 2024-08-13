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
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ManualUnusedDeductionsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationResultDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.adjustments.api.service.UnusedDeductionsService
import uk.gov.justice.digital.hmpps.adjustments.api.service.ValidationService
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/adjustments", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "adjustment-controller", description = "CRUD operations for adjustments.")
class AdjustmentsController(
  val adjustmentsService: AdjustmentsService,
  val validationService: ValidationService,
  val unusedDeductionsService: UnusedDeductionsService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
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
  fun create(@RequestBody adjustments: List<AdjustmentDto>): CreateResponseDto {
    return adjustmentsService.create(adjustments)
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW', 'ADJUSTMENTS__ADJUSTMENTS_RO')")
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW', 'ADJUSTMENTS__ADJUSTMENTS_RO')")
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun update(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
    @RequestBody adjustment: AdjustmentDto,
  ) {
    adjustmentsService.update(adjustmentId, adjustment)
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun restore(
    @Parameter(required = true, description = "The adjustment UUID")
    @RequestBody
    adjustments: RestoreAdjustmentsDto,
  ) {
    adjustmentsService.restore(adjustments)
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun updateEffectiveDays(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
    @RequestBody adjustment: AdjustmentEffectiveDaysDto,
  ) {
    adjustmentsService.updateEffectiveDays(adjustmentId, adjustment)
  }

  @PostMapping("/person/{person}/manual-unused-deductions")
  @Operation(
    summary = "Update the unused deduction days for a person",
    description = "Update the unused deduction days for a person",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Adjustment update"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Adjustment not found"),
    ],
  )
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun setUnusedDaysManually(
    @Parameter(required = true, description = "The person")
    @PathVariable("person")
    person: String,
    @RequestBody manualUnusedDeductionsDto: ManualUnusedDeductionsDto,
  ) {
    unusedDeductionsService.setUnusedDaysManually(person, manualUnusedDeductionsDto)
  }

  @GetMapping("/person/{person}/unused-deductions-result")
  @Operation(
    summary = "Get the unused deductions result",
    description = "Get the unused deductions result",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns result"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "404", description = "Person not found"),
    ],
  )
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW', 'ADJUSTMENTS__ADJUSTMENTS_RO')")
  fun getUnusedDeductionsResult(
    @Parameter(required = true, description = "The person")
    @PathVariable("person")
    person: String,
  ): UnusedDeductionsCalculationResultDto {
    return unusedDeductionsService.getUnusedDeductionsResult(person)
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun delete(
    @Parameter(required = true, description = "The adjustment UUID")
    @PathVariable("adjustmentId")
    adjustmentId: UUID,
  ) {
    adjustmentsService.delete(adjustmentId)
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
  @PreAuthorize("hasAnyRole('ADJUSTMENTS__ADJUSTMENTS_RW')")
  fun validate(@RequestBody adjustment: AdjustmentDto): List<ValidationMessage> {
    return validationService.validate(adjustment)
  }
}
