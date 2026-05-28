package uk.gov.justice.digital.hmpps.adjustments.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${prison.api.url}") private val prisonApiUri: String,
  @Value("\${prison.api.timeout-seconds:90}") private val prisonApiTimeoutSeconds: Long,
  @Value("\${adjudications.api.url}") private val adjudicationsApiUri: String,
  @Value("\${adjudications.api.timeout-seconds:1}") private val adjudicationsApiTimeoutSeconds: Long,
  @Value("\${calculate-release-dates.api.url}") private val calculateReleaseDatesApiUrl: String,
  @Value("\${calculate-release-dates.api.timeout-seconds:90}") private val calculateReleaseDatesApiTimeoutSeconds: Long,
  @Value("\${remand-and-sentencing.api.url}") private val remandAndSentencingApiUrl: String,
  @Value("\${remand-and-sentencing.api.timeout-seconds:90}") private val remandAndSentencingApiTimeoutSeconds: Long,
  @Value("\${prisoner.search.api.url}") private val prisonerSearchApiUrl: String,
  @Value("\${prisoner.search.api.timeout-seconds:90}") private val prisonerSearchApiTimeoutSeconds: Long,
) {

  val customMemorySize = 4 * 1024 * 1024

  @Bean
  fun prisonerSearchApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "hmpps-api",
    url = prisonerSearchApiUrl,
    timeout = Duration.ofSeconds(prisonerSearchApiTimeoutSeconds),
  )

  @Bean
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    val strategies = ExchangeStrategies.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(customMemorySize) }
      .build()
    return builder
      .exchangeStrategies(strategies)
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "hmpps-api",
        url = prisonApiUri,
        timeout = Duration.ofSeconds(prisonApiTimeoutSeconds),
      )
  }

  @Bean
  fun calculateReleaseDatesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "hmpps-api",
    url = calculateReleaseDatesApiUrl,
    timeout = Duration.ofSeconds(calculateReleaseDatesApiTimeoutSeconds),
  )

  @Bean
  fun remandAndSentencingApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "hmpps-api",
    url = remandAndSentencingApiUrl,
    timeout = Duration.ofSeconds(remandAndSentencingApiTimeoutSeconds),
  )

  @Bean
  fun adjudicationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    val strategies = ExchangeStrategies.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(customMemorySize) }
      .build()
    return builder
      .exchangeStrategies(strategies)
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "hmpps-api",
        url = adjudicationsApiUri,
        timeout = Duration.ofSeconds(adjudicationsApiTimeoutSeconds),
      )
  }

  @Bean
  fun oauthApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(oauthApiUrl).build()
}
