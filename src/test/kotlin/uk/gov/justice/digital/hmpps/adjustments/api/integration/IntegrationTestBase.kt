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

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PrisonApiExtension::class, HmppsAuthApiExtension::class, AdjudicationApiExtension::class, CalculateReleaseDatesApiExtension::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAdjustmentsMaintainerAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_ADJUSTMENTS_MAINTAINER", "ROLE_RELEASE_DATES_CALCULATOR"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
  internal fun setViewAdjustmentsAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_VIEW_SENTENCE_ADJUSTMENTS"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
  internal fun setLegacySynchronisationAuth(
    user: String = "Test User",
    roles: List<String> = listOf("ROLE_SENTENCE_ADJUSTMENTS_SYNCHRONISATION"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
}
