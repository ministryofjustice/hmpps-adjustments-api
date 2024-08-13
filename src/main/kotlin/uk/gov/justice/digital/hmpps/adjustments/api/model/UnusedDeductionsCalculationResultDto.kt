package uk.gov.justice.digital.hmpps.adjustments.api.model

import java.time.LocalDateTime

data class UnusedDeductionsCalculationResultDto(

  var person: String = "",

  val calculationAt: LocalDateTime = LocalDateTime.now(),

  val status: UnusedDeductionsCalculationStatus,

)
