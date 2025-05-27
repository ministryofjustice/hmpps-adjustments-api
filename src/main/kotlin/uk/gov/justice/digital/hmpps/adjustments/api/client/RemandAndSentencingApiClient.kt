package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.CourtCase
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.SentenceDetailResponse
import java.util.UUID

@Service
class RemandAndSentencingApiClient(@Qualifier("remandAndSentencingApiWebClient") private val webClient: WebClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun validateCourtCase(courtCaseUUID: UUID): CourtCase? {
    log.info("Getting the court case with UUID $courtCaseUUID")
    return webClient.get()
      .uri("/court-cases/$courtCaseUUID")
      .retrieve()
      .bodyToMono(CourtCase::class.java)
      .block()!!
  }

  fun getSentenceTypeDetails(nomisSentenceReference: String): SentenceDetailResponse? = webClient.get()
    .uri { uriBuilder ->
      uriBuilder.path("/legacy/sentence-type/summary")
        .queryParam("nomisSentenceTypeReference", nomisSentenceReference)
        .build()
    }
    .retrieve()
    .bodyToMono(SentenceDetailResponse::class.java)
    .block()
}
