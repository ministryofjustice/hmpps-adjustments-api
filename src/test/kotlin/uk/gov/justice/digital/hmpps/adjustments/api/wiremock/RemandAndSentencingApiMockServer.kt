package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the remand and sentencing api.
 */
class RemandAndSentencingApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val remandAndSentencingApi = RemandAndSentencingApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    remandAndSentencingApi.start()
    remandAndSentencingApi.stubValidateCourtCases()
    remandAndSentencingApi.stubGetSentenceTypeDetailsForSentenceType("ADIMP_ORA")
    remandAndSentencingApi.stubGetSentenceTypeDetailsForSentenceType("ADIMP")
    remandAndSentencingApi.stubGetSentenceTypeDetailsForSentenceType("LR_LASPO_DR")
    remandAndSentencingApi.stubGetSentenceTypeDetailsForSentenceType("LR")
  }

  override fun beforeEach(context: ExtensionContext) {
    remandAndSentencingApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    remandAndSentencingApi.stop()
  }
}

class RemandAndSentencingApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8335
  }

  fun stubGetSentenceTypeDetailsForSentenceType(sentenceType: String) {
    val isNotRecall = """
                        {
                            "nomisSentenceTypeReference": "ADIMP_ORA",
                            "recall": {
                                "isRecall": false,
                                "type": "NONE",
                                "isFixedTermRecall": false,
                                "lengthInDays": 0
                            },
                            "nomisDescription": "CJA03 Standard Determinate Sentence",
                            "isIndeterminate": false,
                            "nomisActive": true,
                            "nomisExpiryDate": null
                        }
              """
    val isRecall = """
                        {
                            "nomisSentenceTypeReference": "LR_LASPO_DR",
                            "recall": {
                                "isRecall": true,
                                "type": "LR - EDS LASPO Discretionary Release",
                                "isFixedTermRecall": false,
                                "lengthInDays": 0
                            },
                            "nomisDescription": "CJA03 Standard Determinate Sentence",
                            "isIndeterminate": false,
                            "nomisActive": true,
                            "nomisExpiryDate": null
                        }
              """
    val returnValue = if (sentenceType == "ADIMP_ORA" || sentenceType == "ADIMP") isNotRecall else isRecall
    stubFor(
      get(urlPathEqualTo("/legacy/sentence-type/summary"))
        .withQueryParam("nomisSentenceTypeReference", matching(sentenceType))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              returnValue.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubValidateCourtCases() {
    stubFor(
      get("/court-cases/73df3e55-9c5d-487e-959a-5befa13b7123")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                    {                  
                    "prisonerId": "A6160DZ",
                    "courtCaseUuid": "73df3e55-9c5d-487e-959a-5befa13b7123",
                    "status": "ACTIVE",
                    "latestAppearance": {}
                    }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
