package uk.gov.justice.digital.hmpps.adjustments.api.integration

import org.junit.jupiter.api.Test

class NotFoundTest : SqsIntegrationTestBase() {

  @Test
  fun `Resources that aren't found should return 404 - test of the exception handler`() {
    webTestClient.get().uri("/some-url-not-found")
      .headers(setAdjustmentsMaintainerAuth())
      .exchange()
      .expectStatus().isNotFound
  }
}
