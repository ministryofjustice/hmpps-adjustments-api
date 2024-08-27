package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.config.UserContext

@Service
class UnusedDeductionsEventService(
  val unusedDeductionsService: UnusedDeductionsService,
) {

  fun handleAdjustmentMessage(adjustmentEvent: AdjustmentEvent) {
    UserContext.setOverrideUsername("UnusedDeductionsListener")
    val (_, offenderNo, source, unusedDeductions, lastEvent) = adjustmentEvent.additionalInformation
    if (source == "DPS" && !unusedDeductions && lastEvent) {
      calculateUnusedDeductions(offenderNo)
    }
    if (source == "NOMIS") {
      unusedDeductionsService.setStatusToNomisAdjustments(offenderNo)
    }
  }

  fun handlePrisonerSearchEvent(prisonerSearchEvent: PrisonerSearchEvent) {
    UserContext.setOverrideUsername("UnusedDeductionsListener")
    val (categoriesChanged, nomsNumber) = prisonerSearchEvent.additionalInformation
    if (categoriesChanged.contains("SENTENCE")) {
      calculateUnusedDeductions(nomsNumber)
    }
  }

  private fun calculateUnusedDeductions(person: String) {
    unusedDeductionsService.setStatusToInProgress(person)
    try {
      unusedDeductionsService.recalculateUnusedDeductions(person)
    } finally {
      unusedDeductionsService.removeInProgressStatus(person)
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
