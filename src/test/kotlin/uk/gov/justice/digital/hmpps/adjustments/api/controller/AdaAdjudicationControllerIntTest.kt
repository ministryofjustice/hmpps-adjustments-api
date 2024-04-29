package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.model.ProspectiveAdaRejectionDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension

class AdaAdjudicationControllerIntTest : SqsIntegrationTestBase() {

  @Test
  fun `Get adjudications and reject all prospective adas`() {
    var adjudicationDetails = webTestClient
      .get()
      .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/adjudication-details")
      .headers(setAdjustmentsMaintainerAuth())
      .exchange()
      .expectStatus().isOk
      .returnResult(AdaAdjudicationDetails::class.java)
      .responseBody.blockFirst()!!

    assertThat(adjudicationDetails.intercept.type).isEqualTo(InterceptType.PADA)

    adjudicationDetails.prospective.forEach {
      webTestClient
        .post()
        .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/reject-prospective-ada")
        .headers(setAdjustmentsMaintainerAuth())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(ProspectiveAdaRejectionDto(person = PrisonApiExtension.PRISONER_ID, days = it.total!!, dateChargeProved = it.dateChargeProved))
        .exchange()
        .expectStatus().isOk
    }

    adjudicationDetails = webTestClient
      .get()
      .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/adjudication-details")
      .headers(setAdjustmentsMaintainerAuth())
      .exchange()
      .expectStatus().isOk
      .returnResult(AdaAdjudicationDetails::class.java)
      .responseBody.blockFirst()!!

    assertThat(adjudicationDetails.intercept.type).isEqualTo(InterceptType.NONE)
  }

}
