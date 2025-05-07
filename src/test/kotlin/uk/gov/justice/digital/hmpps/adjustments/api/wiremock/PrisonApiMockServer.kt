package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.adjustments.api.listener.UNUSED_DEDUCTIONS_ERROR_PRISONER_ID
import uk.gov.justice.digital.hmpps.adjustments.api.listener.UNUSED_DEDUCTIONS_PRISONER_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.BOOKING_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.RECALL_BOOKING_ID
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.RECALL_PRISONER_ID

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()

    const val BOOKING_ID = 123L
    const val PRISONER_ID = "BCDEFG"

    const val BOOKING_MOVE_NEW_BOOKING_ID = 234L
    const val BOOKING_MOVE_NEW_PRISONER_ID = "NEWPERSON"
    const val BOOKING_MOVE_OLD_BOOKING_ID = 456L
    const val BOOKING_MOVE_OLD_PRISONER_ID = "OLDPERSON"
    const val BOOKING_MOVE_UPDATED_OLD_BOOKING_ID = 789L

    const val RECALL_BOOKING_ID = 321L
    const val RECALL_PRISONER_ID = "CDEFGH"
    const val EARLIEST_SENTENCE_DATE = "2015-03-17"
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubSentencesAndOffences()
    prisonApi.stubGetUnusedDeductionsPrisonerDetails()
    prisonApi.stubGetPrison("LDS", "Leeds")
    prisonApi.stubGetPrison("MRG", "Moorgate")
    prisonApi.stubGetPrison("KMI", "Kirkham (HMP)")
    prisonApi.stubGetPrison("PNI", "Preston (HMP)")
    prisonApi.stubGetPrison("BMI", "Birmingham (HMP)")
    prisonApi.stubGetPrison("ACI", "Arrington (HMP)")
    prisonApi.stubGetRecallPrisonerDetails()
    prisonApi.stubRecallSentenceAndOffences()
    prisonApi.stubGetUnusedDeductionsErrorPrisonerDetails()
    prisonApi.stubG4946VC()
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
      get("/api/offender-sentences/booking/${PrisonApiExtension.BOOKING_ID}/sentences-and-offences").willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
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
        ).withStatus(200),
      ),
    )
  }

  fun stubGetUnusedDeductionsPrisonerDetails(): StubMapping = stubFor(
    get("/api/offenders/$UNUSED_DEDUCTIONS_PRISONER_ID").willReturn(
      aResponse().withHeader("Content-Type", "application/json").withBody(
        """
              {
                 "offenderNo": "$UNUSED_DEDUCTIONS_PRISONER_ID",
                 "bookingId": $BOOKING_ID,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08",
                 "agencyId": "LDS"
              }
        """.trimIndent(),
      ).withStatus(200),
    ),
  )

  fun stubGetUnusedDeductionsErrorPrisonerDetails(): StubMapping = stubFor(
    get("/api/offenders/$UNUSED_DEDUCTIONS_ERROR_PRISONER_ID").willReturn(
      aResponse().withHeader("Content-Type", "application/json").withBody(
        """
              {
                 "offenderNo": "$UNUSED_DEDUCTIONS_ERROR_PRISONER_ID",
                 "bookingId": $BOOKING_ID,
                 "firstName": "Default",
                 "lastName": "Prisoner",
                 "dateOfBirth": "1995-03-08",
                 "agencyId": "LDS"
              }
        """.trimIndent(),
      ).withStatus(200),
    ),
  )

  fun stubGetPrison(prisonId: String, prisonDescription: String): StubMapping = stubFor(
    get("/api/agencies/$prisonId?activeOnly=false").willReturn(
      aResponse().withHeader("Content-Type", "application/json").withBody(
        """
              {
                 "agencyId": "$prisonId",
                 "description": "$prisonDescription"
              }
        """.trimIndent(),
      ).withStatus(200),
    ),
  )

  fun stubGetRecallPrisonerDetails(): StubMapping = stubFor(
    get("/api/offenders/$RECALL_PRISONER_ID").willReturn(
      aResponse().withHeader("Content-Type", "application/json").withBody(
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
      ).withStatus(200),
    ),
  )

  fun stubRecallSentenceAndOffences() {
    stubFor(
      get("/api/offender-sentences/booking/$RECALL_BOOKING_ID/sentences-and-offences").willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
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
        ).withStatus(200),
      ),
    )
  }

  fun stubG4946VC() {
    stubFor(
      get("/api/offenders/G4946VC").willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          G4946VC_PRISONER.trimIndent(),
        ).withStatus(200),
      ),
    )
    stubFor(
      get("/api/offender-sentences/booking/777831/sentences-and-offences").willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          G4946VC_SENTENCES_AND_OFFENCES.trimIndent(),
        ).withStatus(200),
      ),
    )
    stubFor(
      get("/api/court-date-results/by-charge/G4946VC").willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          G4946VC_COURT_DATE_RESULTS.trimIndent(),
        ).withStatus(200),
      ),
    )
  }
}
