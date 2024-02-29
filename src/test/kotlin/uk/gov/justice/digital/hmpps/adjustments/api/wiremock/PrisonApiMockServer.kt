package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.BOOKING_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.PRISONER_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.RECALL_BOOKING_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.RECALL_PRISONER_ID

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
    const val BOOKING_ID = 123L
    const val PRISONER_ID = "BCDEFG"
    const val RECALL_BOOKING_ID = 321L
    const val RECALL_PRISONER_ID = "CDEFGH"
    const val EARLIEST_SENTENCE_DATE = "2015-03-17"
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubSentencesAndOffences()
    prisonApi.stubGetPrisonerDetails()
    prisonApi.stubGetPrison("LDS", "Leeds")
    prisonApi.stubGetPrison("MRG", "Moorgate")
    prisonApi.stubGetRecallPrisonerDetails()
    prisonApi.stubRecallSentenceAndOffences()
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
                    "caseSequence": 9191,
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
  fun stubGetPrisonerDetails(): StubMapping =
    stubFor(
      get("/api/offenders/$PRISONER_ID")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "offenderNo": "$PRISONER_ID",
                 "bookingId": $BOOKING_ID,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08",
                 "agencyId": "LDS"
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

  fun stubGetRecallPrisonerDetails(): StubMapping =
    stubFor(
      get("/api/offenders/$RECALL_PRISONER_ID")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "offenderNo": "$RECALL_PRISONER_ID",
                 "bookingId": $RECALL_BOOKING_ID,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08",
                 "agencyId": "LDS"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  fun stubRecallSentenceAndOffences() {
    stubFor(
      get("/api/offender-sentences/booking/$RECALL_BOOKING_ID/sentences-and-offences")
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
                    "sentenceCalculationType": "LR_LASPO_DR",
                    "sentenceTypeDescription": "Recall",
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
}
