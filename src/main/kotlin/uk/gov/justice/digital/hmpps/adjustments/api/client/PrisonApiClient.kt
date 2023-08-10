package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonerDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getSentencesAndOffences(bookingId: Long): List<SentenceAndOffences> {
    log.info("Requesting sentence terms for bookingId $bookingId")
    return webClient.get()
      .uri("/api/offender-sentences/booking/$bookingId/sentences-and-offences")
      .retrieve()
      .bodyToMono(typeReference<List<SentenceAndOffences>>())
      .block()!!
  }

  fun getPrisonerDetail(nomsId: String): PrisonerDetails {
    log.info("Requesting details for nomsId $nomsId")
    return webClient.get()
      .uri("/api/offenders/$nomsId")
      .retrieve()
      .bodyToMono(typeReference<PrisonerDetails>())
      .block()!!
  }
}
