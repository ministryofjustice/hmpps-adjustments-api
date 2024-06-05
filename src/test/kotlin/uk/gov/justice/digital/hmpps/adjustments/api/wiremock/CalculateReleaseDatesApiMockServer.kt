package uk.gov.justice.digital.hmpps.adjustments.api.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.adjustments.api.listener.UNUSED_DEDUCTIONS_PRISONER_ID

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
    calculateReleaseDatesApi.stubCalculateUnusedDeductions()
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
    private const val WIREMOCK_PORT = 8334
  }

  fun stubCalculateUnusedDeductions() {
    stubFor(
      post("/unused-deductions/$UNUSED_DEDUCTIONS_PRISONER_ID/calculation")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {"unusedDeductions":150}
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
