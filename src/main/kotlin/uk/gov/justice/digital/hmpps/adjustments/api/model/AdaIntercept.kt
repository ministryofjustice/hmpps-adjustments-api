package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType

data class AdaIntercept(
  val type: InterceptType,
  val number: Int,
  val anyProspective: Boolean,
)
