package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.PRISONER_ID

class ThingsToDoControllerIntTest : SqsIntegrationTestBase() {
  @Nested
  inner class GetTests {
    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
    )
    fun `Get things to do for a prisoner`() {
      val thingsToDo = getThingsToDo(PRISONER_ID)

      assertThat(thingsToDo).isEqualTo(ThingsToDo(prisonerId = PRISONER_ID, thingsToDo = listOf(ADA_INTERCEPT)))
    }
  }

  private fun getThingsToDo(prisonerId: String): ThingsToDo? = webTestClient
    .get()
    .uri("/things-to-do/prisoner/$prisonerId")
    .headers(setAdjustmentsRWAuth())
    .header("Active-Caseload", "KMI")
    .exchange()
    .expectStatus().isOk
    .expectBody<ThingsToDo>()
    .returnResult()
    .responseBody
}
