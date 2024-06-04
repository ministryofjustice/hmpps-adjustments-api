package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service

@Service
class UnusedDeductionsEventService(
  val unusedDeductionsService: UnusedDeductionsService,
) {

  fun handleAdjustmentMessage(adjustmentEvent: AdjustmentEvent) {
    val (_, offenderNo, source, unusedDeductions, lastEvent) = adjustmentEvent.additionalInformation
    if (source == "DPS" && !unusedDeductions && lastEvent) {
      unusedDeductionsService.recalculateUnusedDeductions(offenderNo)
    }
  }

  fun handlePrisonerSearchEvent(prisonerSearchEvent: PrisonerSearchEvent) {
    val (categoriesChanged, nomsNumber) = prisonerSearchEvent.additionalInformation
    if (categoriesChanged.contains("SENTENCE")) {
      unusedDeductionsService.recalculateUnusedDeductions(nomsNumber)
    }
  }
}

data class AdjustmentAdditionalInformation(
  val id: String,
  val offenderNo: String,
  val source: String,
  val unusedDeductions: Boolean = false,
  val lastEvent: Boolean = true,
)

data class AdjustmentEvent(
  val additionalInformation: AdjustmentAdditionalInformation,
)

data class PrisonerSearchAdditionalInformation(
  val categoriesChanged: List<String>,
  val nomsNumber: String,
)

data class PrisonerSearchEvent(
  val additionalInformation: PrisonerSearchAdditionalInformation,
)
