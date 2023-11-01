package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class CalculateReleaseDatesApiClient(@Qualifier("calculateReleaseDatesApiWebClient") private val webClient: WebClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun calculateUnusedDeductions(prisonerId: String) {
    log.info("Calculating unused deductions for $prisonerId")
    webClient.post()
      .uri("/unused-deductions/$prisonerId")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .block()!!
  }
}
