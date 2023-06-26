package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
    const val BOOKING_ID = 123L
  }
  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubSentencesAndOffences()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8332
  }

  fun stubSentencesAndOffences() {
    stubFor(
      get("/api/offender-sentences/booking/${PrisonApiExtension.BOOKING_ID}/sentences-and-offences")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                  {
                    "bookingId": 123,
                    "sentenceSequence": 1,
                    "sentenceStatus": "A",
                    "sentenceCategory": "2003",
                    "sentenceCalculationType": "ADIMP_ORA",
                    "sentenceTypeDescription": "Standard Determinate",
                    "sentenceDate": "2015-03-17",
                    "terms": [{
                      "years": 0,
                      "months": 20,
                      "weeks": 0,
                      "days": 0
                    }],
                    "offences": [
                      {
                        "offenderChargeId": 9991,
                        "offenceStartDate": "2015-03-17",
                        "offenceCode": "GBH",
                        "offenceDescription": "Grievous bodily harm"
                      }
                    ]
                  }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
