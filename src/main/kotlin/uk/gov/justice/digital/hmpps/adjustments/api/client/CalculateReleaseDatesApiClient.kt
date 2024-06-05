package uk.gov.justice.digital.hmpps.adjustments.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionCalculationResponse

@Service
class CalculateReleaseDatesApiClient(@Qualifier("calculateReleaseDatesApiWebClient") private val webClient: WebClient) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun calculateUnusedDeductions(adjustments: List<AdjustmentDto>, prisonerId: String): UnusedDeductionCalculationResponse {
    log.info("Calculating unused deductions for $prisonerId")
    return webClient.post()
      .uri("/unused-deductions/$prisonerId/calculation")
      .bodyValue(adjustments)
      .retrieve()
      .bodyToMono(typeReference<UnusedDeductionCalculationResponse>())
      .block()!!
  }
}
