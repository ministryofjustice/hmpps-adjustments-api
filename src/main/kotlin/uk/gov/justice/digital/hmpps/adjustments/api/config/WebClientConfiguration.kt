package uk.gov.justice.digital.hmpps.adjustments.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
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
  @Value("\${adjudications.api.timeout-seconds:90}") private val adjudicationsApiTimeoutSeconds: Long,
  @Value("\${calculate-release-dates.api.url}") private val calculateReleaseDatesApiUrl: String,
  @Value("\${calculate-release-dates.api.timeout-seconds:90}") private val calculateReleaseDatesApiTimeoutSeconds: Long,
  @Value("\${remand-and-sentencing.api.url}") private val remandAndSentencingApiUrl: String,
  @Value("\${remand-and-sentencing.api.timeout-seconds:90}") private val remandAndSentencingApiTimeoutSeconds: Long,
) {

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    return builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "hmpps-api",
      url = prisonApiUri,
      timeout = Duration.ofSeconds(prisonApiTimeoutSeconds),
    )
  }

  @Bean
  fun calculateReleaseDatesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    return builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "hmpps-api",
      url = calculateReleaseDatesApiUrl,
      timeout = Duration.ofSeconds(calculateReleaseDatesApiTimeoutSeconds),
    )
  }

  @Bean
  fun remandAndSentencingApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    return builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "hmpps-api",
      url = remandAndSentencingApiUrl,
      timeout = Duration.ofSeconds(remandAndSentencingApiTimeoutSeconds),
    )
  }

  @Bean
  fun adjudicationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    val size = 4 * 1024 * 1024
    val strategies = ExchangeStrategies.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(size) }
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

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
    clientRegistrationRepository,
    oAuth2AuthorizedClientService,
  ).apply {
    setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
  }

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthApiUrl).build()
  }
}
