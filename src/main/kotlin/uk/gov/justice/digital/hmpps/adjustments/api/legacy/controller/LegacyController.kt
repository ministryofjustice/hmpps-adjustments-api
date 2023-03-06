package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import java.util.UUID

@RestController
@RequestMapping("/adjustments", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = ["application/nomis_offence+json"])
class LegacyController {

  @PostMapping("/")
  fun create(@RequestBody adjustment: LegacyAdjustment): LegacyAdjustmentCreatedResponse {

  }

  @GetMapping("/{adjustmentId}")
  fun get(@PathVariable("adjustmentId") adjustmentId: UUID): LegacyAdjustment {
  }

  @PutMapping("/{adjustmentId}")
  fun update(@PathVariable("adjustmentId") adjustmentId: UUID,
             @RequestBody adjustment: LegacyAdjustment): LegacyAdjustment {
  }

  @DeleteMapping("/{adjustmentId}")
  fun delete(@PathVariable("adjustmentId") adjustmentId: UUID): LegacyAdjustment {
  }


}