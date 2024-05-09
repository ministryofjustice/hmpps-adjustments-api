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
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${prison.api.url}") private val prisonApiUri: String,
  @Value("\${adjudications.api.url}") private val adjudicationsApiUri: String,
) {
  @Bean
  fun prisonApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(prisonApiUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun systemPrisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(prisonApiUri)
      .filter(filter)
      .build()
  }

  @Bean
  fun adjudicationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(adjudicationsApiUri)
      .filter(filter)
      .build()
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
