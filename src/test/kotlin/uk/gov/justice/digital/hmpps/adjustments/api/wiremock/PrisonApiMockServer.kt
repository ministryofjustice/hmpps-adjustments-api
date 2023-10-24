package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.NO_ACTIVE_SENTENCE_PRISONER_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.PRISONER_ID

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
    const val BOOKING_ID = 123L
    const val PRISONER_ID = "BCDEFG"
    const val NO_ACTIVE_SENTENCE_PRISONER_ID = "NO_SENTENCES"
    const val NO_ACTIVE_SENTENCE_BOOKING_ID = 2L
    const val EARLIEST_SENTENCE_DATE = "2015-03-17"
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubNoActiveSentences()
    prisonApi.stubGetNoActiveSentencePrisoner()
    prisonApi.stubSentencesAndOffences()
    prisonApi.stubGetPrisonerDetails()
    prisonApi.stubGetPrison("LDS", "Leeds")
    prisonApi.stubGetPrison("MRG", "Moorgate")
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
                    "sentenceDate": "${PrisonApiExtension.EARLIEST_SENTENCE_DATE}",
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
  fun stubNoActiveSentences() {
    stubFor(
      get("/api/offender-sentences/booking/${PrisonApiExtension.NO_ACTIVE_SENTENCE_BOOKING_ID}/sentences-and-offences")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                  {
                    "bookingId": 123,
                    "sentenceSequence": 1,
                    "sentenceStatus": "I",
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
  fun stubGetPrisonerDetails(): StubMapping =
    stubFor(
      get("/api/offenders/$PRISONER_ID")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "offenderNo": "default",
                 "bookingId": 123,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  fun stubGetNoActiveSentencePrisoner(): StubMapping =
    stubFor(
      get("/api/offenders/$NO_ACTIVE_SENTENCE_PRISONER_ID")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "offenderNo": "default",
                 "bookingId": 2,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  fun stubGetPrison(prisonId: String, prisonDescription: String): StubMapping =
    stubFor(
      get("/api/agencies/$prisonId?activeOnly=false")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "agencyId": "$prisonId",
                 "description": "$prisonDescription"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
}
