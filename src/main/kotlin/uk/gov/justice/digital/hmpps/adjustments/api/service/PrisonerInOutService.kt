package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.SystemPrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.service.LegacyService

@Service
class PrisonerInOutService(
  private val systemPrisonApiClient: SystemPrisonApiClient,
  private val legacyService: LegacyService,
) {

  fun handleRelease(event: PrisonerEvent) {
    if (event.additionalInformation.reason == RELEASE_REASON) {
      val prisoner = systemPrisonApiClient.getPrisonerDetail(event.additionalInformation.nomsNumber)
      legacyService.setReleased(prisoner)
    }
  }

  fun handleReceived(event: PrisonerEvent) {
    if (event.additionalInformation.reason == ADMISSION_REASON) {
      val prisoner = systemPrisonApiClient.getPrisonerDetail(event.additionalInformation.nomsNumber)
      legacyService.setAdmission(prisoner)
    }
  }
}

const val RELEASE_REASON = "RELEASED"
const val ADMISSION_REASON = "NEW_ADMISSION"

data class PrisonerEvent(
  val additionalInformation: PrisonerAdditionalInformation,
)

data class PrisonerAdditionalInformation(
  val nomsNumber: String,
  val reason: String,
)