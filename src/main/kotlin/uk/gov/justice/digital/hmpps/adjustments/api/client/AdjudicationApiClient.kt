package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.AdjudicationResponse

@Service
class AdjudicationApiClient(@Qualifier("adjudicationApiWebClient") private val webClient: WebClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getAdjudications(person: String): AdjudicationResponse {
    log.info("Requesting adjudications for $person")
    return webClient.get()
      .uri("/reported-adjudications/bookings/prisoner/$person?page=0&size=1000&ada=true&pada=true")
      .retrieve()
      .bodyToMono(AdjudicationResponse::class.java)
      .block()!!
  }
}
