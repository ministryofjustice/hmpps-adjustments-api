package uk.gov.justice.digital.hmpps.adjustments.api.listener

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

const val TAGGED_BAIL_ID = "a4cef857-cb1e-43e4-b905-13e85fee7538"
const val REMAND_ID = "29044a3c-6261-4681-af9e-ef2a84c51a22"
const val OFFENDER_NUMBER = "A1234TT"
const val BOOKING_ID = 987651L

class UnusedDeductionsEventListenerIntTest : SqsIntegrationTestBase() {

  @Test
  fun handleAdjustmentEvent() {
    val eventType = "release-date-adjustments.adjustment.inserted"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(sentencingAdjustmentMessagePayload(TAGGED_BAIL_ID, OFFENDER_NUMBER, eventType, "DPS"))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
    }

//    await untilAsserted {
//      AdjustmentsApiExtension.adjustmentsApi.verify(
//        WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments"))
//          .withRequestBody(WireMock.matchingJsonPath("[0].days", WireMock.equalTo("10"))),
//      )
//      AdjustmentsApiExtension.adjustmentsApi.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments/$REMAND_ID/effective-days")))
//    }
  }

  fun sentencingAdjustmentMessagePayload(adjustmentId: String, nomsNumber: String, eventType: String, source: String = "DPS") =
    """{"eventType":"$eventType", "additionalInformation": {"id":"$adjustmentId", "offenderNo": "$nomsNumber", "source": "$source"}}"""

  @Test
  fun handlePrisonerSearchEvent() {
    val eventType = "prisoner-offender-search.prisoner.updated"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerSearchPayload(OFFENDER_NUMBER, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
    }

//    await untilAsserted {
//      AdjustmentsApiExtension.adjustmentsApi.verify(
//        WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments"))
//          .withRequestBody(WireMock.matchingJsonPath("[0].days", WireMock.equalTo("10"))),
//      )
//      AdjustmentsApiExtension.adjustmentsApi.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments/$REMAND_ID/effective-days")))
//    }
  }

  private fun prisonerSearchPayload(offenderNumber: String, eventType: String): String? =
    """{"eventType":"$eventType", "additionalInformation": {"nomsNumber": "$offenderNumber", "categoriesChanged": ["SENTENCE"]}}"""
}
