package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the prisoner-search-api.
 */
class PrisonerSearchApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearchApi = PrisonerSearchApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearchApi.start()
    prisonerSearchApi.stubGetPrisonerDetails(".*", PrisonApiExtension.BOOKING_ID)
    prisonerSearchApi.stubGetPrisonerDetails("G4946VC", 777831)
    prisonerSearchApi.stubGetPrisonerDetails(PrisonApiExtension.PRISONER_ID, PrisonApiExtension.BOOKING_ID)
    prisonerSearchApi.stubGetPrisonerDetails(PrisonApiExtension.BOOKING_MOVE_NEW_PRISONER_ID, PrisonApiExtension.BOOKING_MOVE_OLD_BOOKING_ID)
    prisonerSearchApi.stubGetPrisonerDetails(PrisonApiExtension.BOOKING_MOVE_OLD_PRISONER_ID, PrisonApiExtension.BOOKING_MOVE_UPDATED_OLD_BOOKING_ID)
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearchApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearchApi.stop()
  }
}

class PrisonerSearchApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8336
  }

  fun stubGetPrisonerDetails(prisonerId: String, bookingId: Long): StubMapping =
    stubFor(
      get(urlMatching("/prisoner-search-api/prisoner/$prisonerId"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "prisonerNumber": "$prisonerId",
                 "bookingId": $bookingId,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08",
                 "prisonId": "LDS"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
}
