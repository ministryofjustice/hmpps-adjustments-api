package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.service.LegacyService

@Service
class PrisonerEventService(
  private val prisonApiClient: PrisonApiClient,
  private val legacyService: LegacyService,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun handleRelease(event: PrisonerEvent) {
    if (event.additionalInformation.reason == RELEASE_REASON) {
      log.info("Handling release of ${event.additionalInformation.nomsNumber}")
      val prisoner = prisonApiClient.getPrisonerDetail(event.additionalInformation.nomsNumber)
      legacyService.setReleased(prisoner)
    }
  }

  fun handleReceived(event: PrisonerEvent) {
    log.info("Handling admission of ${event.additionalInformation.nomsNumber}")
    val prisoner = prisonApiClient.getPrisonerDetail(event.additionalInformation.nomsNumber)
    legacyService.setAdmission(prisoner)
  }

  fun handlePrisonerMerged(event: PrisonerMergedEvent) {
    log.info("Handling merge of ${event.additionalInformation.removedNomsNumber} to ${event.additionalInformation.nomsNumber}")
    legacyService.prisonerMerged(event.additionalInformation.nomsNumber, event.additionalInformation.removedNomsNumber)
  }
}

const val RELEASE_REASON = "RELEASED"
val ADMISSION_REASONS = listOf("NEW_ADMISSION", "READMISSION", "READMISSION_SWITCH_BOOKING")

data class PrisonerEvent(
  val additionalInformation: PrisonerAdditionalInformation,
)
data class PrisonerAdditionalInformation(
  val nomsNumber: String,
  val reason: String,
)
data class PrisonerMergedEvent(
  val additionalInformation: PrisonerMergedAdditionalInformation,
)
data class PrisonerMergedAdditionalInformation(
  val removedNomsNumber: String,
  val nomsNumber: String,
)
