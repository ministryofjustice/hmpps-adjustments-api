package uk.gov.justice.digital.hmpps.hmppsadjustmentsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsAdjustmentsApi

fun main(args: Array<String>) {
  runApplication<HmppsAdjustmentsApi>(*args)
}
