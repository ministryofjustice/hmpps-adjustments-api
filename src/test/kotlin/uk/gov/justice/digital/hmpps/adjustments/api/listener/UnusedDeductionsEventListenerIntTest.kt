package uk.gov.justice.digital.hmpps.adjustments.api.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

const val TAGGED_BAIL_ID = "5d2c10d0-0a31-49d1-93a9-52213bb344a5"
const val REMAND_ID = "72ba4684-5674-4ada-9aa4-41011ff23451"
const val UNUSED_DEDUCTIONS_PRISONER_ID = "A1234TT"
const val BOOKING_ID = 987651L

class UnusedDeductionsEventListenerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  @Sql(
    "classpath:test_data/reset-data.sql",
    "classpath:test_data/insert-unused-deduction-adjustments.sql",
  )
  fun handleAdjustmentEvent() {
    val eventType = "release-date-adjustments.adjustment.inserted"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(sentencingAdjustmentMessagePayload(TAGGED_BAIL_ID, UNUSED_DEDUCTIONS_PRISONER_ID, eventType, "DPS"))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustments = adjustmentRepository.findByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      val remand = adjustments.find { it.id.toString() == REMAND_ID }!!
      val taggedBail = adjustments.find { it.id.toString() == TAGGED_BAIL_ID }!!
      val unusedDeductions = adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }!!

      assertThat(remand.daysCalculated).isEqualTo(100)
      assertThat(remand.effectiveDays).isEqualTo(0)

      assertThat(taggedBail.days).isEqualTo(100)
      assertThat(taggedBail.effectiveDays).isEqualTo(0)

      assertThat(unusedDeductions.days).isEqualTo(150)
    }
  }

  fun sentencingAdjustmentMessagePayload(adjustmentId: String, nomsNumber: String, eventType: String, source: String = "DPS") =
    """{"eventType":"$eventType", "additionalInformation": {"id":"$adjustmentId", "offenderNo": "$nomsNumber", "source": "$source"}}"""

  @Test
  @Sql(
    "classpath:test_data/reset-data.sql",
    "classpath:test_data/insert-unused-deduction-adjustments.sql",
  )
  fun handlePrisonerSearchEvent() {
    val eventType = "prisoner-offender-search.prisoner.updated"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerSearchPayload(UNUSED_DEDUCTIONS_PRISONER_ID, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustments = adjustmentRepository.findByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      val remand = adjustments.find { it.id.toString() == REMAND_ID }!!
      val taggedBail = adjustments.find { it.id.toString() == TAGGED_BAIL_ID }!!
      val unusedDeductions = adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }!!

      assertThat(remand.daysCalculated).isEqualTo(100)
      assertThat(remand.effectiveDays).isEqualTo(0)

      assertThat(taggedBail.days).isEqualTo(100)
      assertThat(taggedBail.effectiveDays).isEqualTo(0)

      assertThat(unusedDeductions.days).isEqualTo(150)
    }
  }

  private fun prisonerSearchPayload(offenderNumber: String, eventType: String): String? =
    """{"eventType":"$eventType", "additionalInformation": {"nomsNumber": "$offenderNumber", "categoriesChanged": ["SENTENCE"]}}"""
}
