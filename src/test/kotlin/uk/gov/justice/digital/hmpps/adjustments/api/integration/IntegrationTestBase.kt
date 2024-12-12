package uk.gov.justice.digital.hmpps.adjustments.api.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.adjustments.api.integration.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.AdjudicationApiExtension
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.CalculateReleaseDatesApiExtension
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.RemandAndSentencingApiExtension

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PrisonApiExtension::class, HmppsAuthApiExtension::class, AdjudicationApiExtension::class, CalculateReleaseDatesApiExtension::class, RemandAndSentencingApiExtension::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAdjustmentsRWAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_ADJUSTMENTS__ADJUSTMENTS_RW"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
  internal fun setAdjustmentsROAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_ADJUSTMENTS__ADJUSTMENTS_RO"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
  internal fun setLegacySynchronisationAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_SENTENCE_ADJUSTMENTS_SYNCHRONISATION"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
}
