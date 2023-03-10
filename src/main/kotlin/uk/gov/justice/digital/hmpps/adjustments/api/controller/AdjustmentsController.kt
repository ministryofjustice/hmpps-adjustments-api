package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDetailsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsService
import java.util.UUID

@RestController
@RequestMapping("/adjustments", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdjustmentsController(
  val adjustmentsService: AdjustmentsService
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody adjustment: AdjustmentDetailsDto): CreateResponseDto {
    return adjustmentsService.create(adjustment)
  }

  @GetMapping("", params = ["person"])
  fun findByPerson(@RequestParam("person") person: String): List<AdjustmentDto> {
    return adjustmentsService.findByPerson(person)
  }
  @GetMapping("/{adjustmentId}")
  fun get(@PathVariable("adjustmentId") adjustmentId: UUID): AdjustmentDetailsDto {
    return adjustmentsService.get(adjustmentId)
  }

  @PutMapping("/{adjustmentId}")
  fun update(
    @PathVariable("adjustmentId") adjustmentId: UUID,
    @RequestBody adjustment: AdjustmentDetailsDto
  ) {
    adjustmentsService.update(adjustmentId, adjustment)
  }

  @DeleteMapping("/{adjustmentId}")
  fun delete(@PathVariable("adjustmentId") adjustmentId: UUID) {
    adjustmentsService.delete(adjustmentId)
  }
}
