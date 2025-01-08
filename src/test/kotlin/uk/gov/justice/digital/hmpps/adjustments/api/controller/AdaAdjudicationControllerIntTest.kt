package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.model.ProspectiveAdaRejectionDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate

class AdaAdjudicationControllerIntTest : SqsIntegrationTestBase() {

  @Test
  fun `Get adjudications and reject all prospective adas`() {
    var adjudicationDetails = webTestClient
      .get()
      .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/adjudication-details")
      .headers(setAdjustmentsRWAuth())
      .header("Active-Caseload", "KMI")
      .exchange()
      .expectStatus().isOk
      .returnResult(AdaAdjudicationDetails::class.java)
      .responseBody.blockFirst()!!

    assertThat(adjudicationDetails.intercept.type).isEqualTo(InterceptType.PADAS)

    adjudicationDetails.prospective.forEach {
      webTestClient
        .post()
        .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/reject-prospective-ada")
        .headers(setAdjustmentsRWAuth())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(ProspectiveAdaRejectionDto(person = PrisonApiExtension.PRISONER_ID, days = it.total!!, dateChargeProved = it.dateChargeProved))
        .exchange()
        .expectStatus().isOk
    }

    adjudicationDetails = webTestClient
      .get()
      .uri("/adjustments/additional-days/${PrisonApiExtension.PRISONER_ID}/adjudication-details")
      .headers(setAdjustmentsRWAuth())
      .header("Active-Caseload", "KMI")
      .exchange()
      .expectStatus().isOk
      .returnResult(AdaAdjudicationDetails::class.java)
      .responseBody.blockFirst()!!

    assertThat(adjudicationDetails.intercept.type).isEqualTo(InterceptType.NONE)
  }

  @Test
  fun `Get adjudications with a recall`() {
    var adjudicationDetails = webTestClient
      .get()
      .uri("/adjustments/additional-days/G4946VC/adjudication-details")
      .headers(setAdjustmentsRWAuth())
      .header("Active-Caseload", "KMI")
      .exchange()
      .expectStatus().isOk
      .returnResult(AdaAdjudicationDetails::class.java)
      .responseBody.blockFirst()!!

    assertThat(adjudicationDetails.intercept.type).isEqualTo(InterceptType.UPDATE)
    assertThat(adjudicationDetails.totalAwaitingApproval).isEqualTo(29)
    assertThat(adjudicationDetails.recallWithMissingOutcome).isFalse
    assertThat(adjudicationDetails.earliestNonRecallSentenceDate).isEqualTo(LocalDate.of(2014, 2, 14))
    assertThat(adjudicationDetails.earliestRecallDate).isEqualTo(LocalDate.of(2010, 9, 12))
  }
}
