package uk.gov.justice.digital.hmpps.adjustments.api.client

import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Prison
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonerDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  suspend fun getSentencesAndOffences(bookingId: Long): Flow<SentenceAndOffences> {
    log.info("Requesting sentence terms for bookingId $bookingId")
    return webClient.get()
      .uri("/api/offender-sentences/booking/$bookingId/sentences-and-offences")
      .retrieve()
      .bodyToFlow()
  }

  suspend fun getPrisonerDetail(nomsId: String): PrisonerDetails {
    log.info("Requesting details for nomsId $nomsId")
    return webClient.get()
      .uri("/api/offenders/$nomsId")
      .retrieve()
      .awaitBody()
  }

  suspend fun getPrison(prisonId: String): Prison {
    log.info("Requesting details for prisonId $prisonId")
    return webClient.get()
      .uri("/api/agencies/$prisonId?activeOnly=false")
      .retrieve()
      .awaitBody()
  }
}
