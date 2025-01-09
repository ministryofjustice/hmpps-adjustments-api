package uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays

import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType

data class AdaIntercept(
  val type: InterceptType,
  val number: Int,
  val anyProspective: Boolean,
  val messageArguments: List<String> = listOf(),
) {
  val message: String?
    get() = type.message?.format(*messageArguments.toTypedArray())
}
