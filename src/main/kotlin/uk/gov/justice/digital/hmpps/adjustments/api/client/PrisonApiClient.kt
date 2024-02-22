package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Prison
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

  fun getPrison(prisonId: String): Prison {
    log.info("Requesting details for prisonId $prisonId")
    return webClient.get()
      .uri("/api/agencies/$prisonId?activeOnly=false")
      .retrieve()
      .bodyToMono(typeReference<Prison>())
      .block()!!
  }

  fun getAdjudications(nomsId: String): AdjudicationSearchResponse {
    log.info("Requesting details for nomsId $nomsId")
    return webClient.get()
      .uri("/api/offenders/$nomsId/adjudications")
      .header("Page-Limit", "10000")
      .retrieve()
      .bodyToMono(typeReference<AdjudicationSearchResponse>())
      .block()!!
  }

  fun getAdjudication(nomsId: String, adjudicationNumber: Long): AdjudicationDetail {
    log.info("Requesting adjudication for nomsId $nomsId and adjudicationNumber $adjudicationNumber")
    return webClient.get()
      .uri("/api/offenders/$nomsId/adjudications/$adjudicationNumber")
      .retrieve()
      .bodyToMono(typeReference<AdjudicationDetail>())
      .block()!!
  }
}
