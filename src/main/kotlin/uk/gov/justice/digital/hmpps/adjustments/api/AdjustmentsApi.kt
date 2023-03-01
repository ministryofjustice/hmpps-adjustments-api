package uk.gov.justice.digital.hmpps.adjustments.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class AdjustmentsApi

fun main(args: Array<String>) {
  runApplication<AdjustmentsApi>(*args)
}
