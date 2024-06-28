package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension.Companion.PRISONER_ID

/*
    This class mocks the adjudication-api.
 */
class AdjudicationApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val adjudicationApi = AdjudicationsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    adjudicationApi.start()
    adjudicationApi.stubAdjudications()
    adjudicationApi.stubG4946VC()
  }

  override fun beforeEach(context: ExtensionContext) {
    adjudicationApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    adjudicationApi.stop()
  }
}

class AdjudicationsApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8333
  }

  fun stubAdjudications() {
    stubFor(
      get("/reported-adjudications/bookings/prisoner/${PRISONER_ID}?page=0&size=1000&ada=true&pada=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                 "content":[
                    {
                       "chargeNumber":"1468919-1",
                       "prisonerNumber":"G3293GQ",
                       "status":"CHARGE_PROVED",
                       "outcomes":[
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2017-01-19T09:43:00",
                                "agencyId":"KMI"
                             }
                          }
                       ],
                       "punishments":[
                          {
                             "type":"PROSPECTIVE_DAYS",
                             "schedule":{
                                "duration":10,
                                "suspendedUntil":null
                             },
                             "consecutiveChargeNumber":null
                          }
                       ]
                    },
                    {
                       "chargeNumber":"998947-1",
                       "prisonerNumber":"G3293GQ",
                       "status":"CHARGE_PROVED",
                       "outcomes":[
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-10-16T09:58:00",
                                "agencyId":"DNI"
                             }
                          },
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-10-23T14:18:00",
                                "agencyId":"DNI"
                             }
                          },
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-11-24T13:31:00",
                                "agencyId":"DNI"
                             }
                          }
                       ],
                       "punishments":[
                          {
                             "type":"ADDITIONAL_DAYS",
                             "schedule":{
                                "duration":21,
                                "suspendedUntil":null
                             },
                             "consecutiveChargeNumber":null
                          }
                       ]
                    },
                    {
                       "chargeNumber":"998946-1",
                       "prisonerNumber":"G3293GQ",
                       "status":"CHARGE_PROVED",
                       "outcomes":[
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-10-16T09:57:00",
                                "agencyId":"DNI"
                             }
                          },
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-10-23T14:17:00",
                                "agencyId":"DNI"
                             }
                          },
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2014-11-24T13:30:00",
                                "agencyId":"DNI"
                             }
                          }
                       ],
                       "punishments":[
                          {
                             "type":"ADDITIONAL_DAYS",
                             "schedule":{
                                "duration":21,
                                "suspendedUntil":null
                             },
                             "consecutiveChargeNumber":null
                          }
                       ]
                    },
                    {
                       "chargeNumber":"104841-1",
                       "prisonerNumber":"G3293GQ",
                       "status":"CHARGE_PROVED",
                       "outcomes":[
                          {
                             "hearing":{
                                "dateTimeOfHearing":"2000-07-25T00:00:00",
                                "agencyId":"PNI"
                             }
                          }
                       ],
                       "punishments":[
                          {
                             "type":"PRIVILEGE",
                             "schedule":{
                                "duration":7,
                                "suspendedUntil":null
                             },
                             "consecutiveChargeNumber":null
                          },
                          {
                             "type":"PROSPECTIVE_DAYS",
                             "schedule":{
                                "duration":7,
                                "suspendedUntil":null
                             },
                             "consecutiveChargeNumber":null
                          }
                       ]
                    }
                 ]
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubG4946VC() {
    stubFor(
      get("/reported-adjudications/bookings/prisoner/G4946VC?page=0&size=1000&ada=true&pada=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              G4946VC_ADJUDICATIONS.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
