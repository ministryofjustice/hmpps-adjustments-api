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

  fun handleReceived(event: PrisonerEvent) {
    log.info("Handling admission of ${event.additionalInformation.nomsNumber}")
    val prisoner = prisonApiClient.getPrisonerDetail(event.additionalInformation.nomsNumber)
    legacyService.setAdmission(prisoner)
  }

  fun handleBookingMoved(event: PrisonerBookingMovedEvent) {
    log.info("Handling booking moved from ${event.additionalInformation.movedFromNomsNumber} to ${event.additionalInformation.movedToNomsNumber}")
    legacyService.moveBooking(
      event.additionalInformation.bookingId,
      event.additionalInformation.movedFromNomsNumber,
      event.additionalInformation.movedToNomsNumber,
    )
  }

  fun handlePrisonerMerged(event: PrisonerMergedEvent) {
    log.info("Handling merge of ${event.additionalInformation.removedNomsNumber} to ${event.additionalInformation.nomsNumber}")
    legacyService.prisonerMerged(event.additionalInformation.nomsNumber, event.additionalInformation.removedNomsNumber)
  }
}

const val RELEASE_REASON = "RELEASED"

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

data class PrisonerBookingMovedEvent(
  val additionalInformation: PrisonerBookingMovedAdditionalInformation,
  val eventType: String,
  val occurredAt: String,
  val personReference: PersonReference,
  val publishedAt: String,
  val version: Int,
)

data class PrisonerBookingMovedAdditionalInformation(
  val bookingId: String,
  val movedFromNomsNumber: String,
  val movedToNomsNumber: String,
  val bookingStartDateTime: String,
)

data class PersonReference(
  val identifiers: List<Identifier>,
)

data class Identifier(
  val type: String,
  val value: String,
)
