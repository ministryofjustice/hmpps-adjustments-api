package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the calculate release dates api.
 */
class CalculateReleaseDatesApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val calculateReleaseDatesApi = CalculateReleaseDatesApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    calculateReleaseDatesApi.start()
    calculateReleaseDatesApi.stubUnusedDeductionCalculation()
  }

  override fun beforeEach(context: ExtensionContext) {
    calculateReleaseDatesApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    calculateReleaseDatesApi.stop()
  }
}

class CalculateReleaseDatesApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8333
  }

  fun stubUnusedDeductionCalculation() {
    stubFor(
      post(WireMock.urlPathMatching("/unused-deductions/.*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
             {  }
               
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
