package uk.gov.justice.digital.hmpps.adjustments.api.client

import jakarta.xml.bind.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.config.UserContext
import uk.gov.justice.digital.hmpps.adjustments.api.config.UserContextFilter
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.AdjudicationResponse

@Service
class AdjudicationApiClient(@Qualifier("adjudicationApiWebClient") private val webClient: WebClient) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getAdjudications(person: String): AdjudicationResponse {
    log.info("Requesting adjudications for $person")
    // TODO This adjudications endpoint needs 'active caseload' passed in as a header but it doesnt actually use it, maybe pass in a dummy val?
    val caseloadId = UserContext.getActiveCaseloadId() ?: throw ValidationException("no active caseload set")
    return webClient.get()
      .uri("/reported-adjudications/bookings/prisoner/$person?page=0&size=1000&ada=true&pada=true")
      .header(UserContextFilter.ACTIVE_CASELOAD, caseloadId)
      .retrieve()
      .bodyToMono(AdjudicationResponse::class.java)
      .block()!!
  }
}
