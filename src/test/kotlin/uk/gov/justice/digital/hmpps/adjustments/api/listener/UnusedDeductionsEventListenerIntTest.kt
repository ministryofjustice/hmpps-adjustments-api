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
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationResultDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationStatus
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.UnusedDeductionsCalculationResultRepository
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.CalculateReleaseDatesApiExtension
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

const val TAGGED_BAIL_ID = "5d2c10d0-0a31-49d1-93a9-52213bb344a5"
const val REMAND_ID = "72ba4684-5674-4ada-9aa4-41011ff23451"
const val UNUSED_DEDUCTIONS_PRISONER_ID = "A1234TT"
const val UNUSED_DEDUCTIONS_ERROR_PRISONER_ID = "A1234TR"

class UnusedDeductionsEventListenerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  private lateinit var unusedDeductionsCalculationResultRepository: UnusedDeductionsCalculationResultRepository

  @Test
  @Sql(
    "classpath:test_data/reset-data.sql",
    "classpath:test_data/insert-unused-deduction-adjustments.sql",
  )
  fun handleAdjustmentEvent() {
    CalculateReleaseDatesApiExtension.calculateReleaseDatesApi.stubCalculateUnusedDeductions()
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
      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      assertThat(result).isNotNull
      assertThat(result!!.status).isEqualTo(UnusedDeductionsCalculationStatus.IN_PROGRESS)
    }

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
      val adjustments = adjustmentRepository.findByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      val remand = adjustments.find { it.id.toString() == REMAND_ID }!!
      val taggedBail = adjustments.find { it.id.toString() == TAGGED_BAIL_ID }!!
      val unusedDeductions = adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }!!

      assertThat(remand.daysCalculated).isEqualTo(100)
      assertThat(remand.effectiveDays).isEqualTo(0)

      assertThat(taggedBail.days).isEqualTo(100)
      assertThat(taggedBail.effectiveDays).isEqualTo(50)

      assertThat(unusedDeductions.days).isEqualTo(150)

      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      assertThat(result).isNotNull
      assertThat(result!!.status).isEqualTo(UnusedDeductionsCalculationStatus.CALCULATED)

      val resultDto = webTestClient
        .get()
        .uri("/adjustments/person/$UNUSED_DEDUCTIONS_PRISONER_ID/unused-deductions-result")
        .headers(setAdjustmentsRWAuth())
        .exchange()
        .expectStatus().isOk
        .returnResult(UnusedDeductionsCalculationResultDto::class.java)
        .responseBody.blockFirst()!!

      assertThat(resultDto.status).isEqualTo(UnusedDeductionsCalculationStatus.CALCULATED)
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
    CalculateReleaseDatesApiExtension.calculateReleaseDatesApi.stubCalculateUnusedDeductions()
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
      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      assertThat(result).isNotNull
      assertThat(result!!.status).isEqualTo(UnusedDeductionsCalculationStatus.IN_PROGRESS)
    }

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
      val adjustments = adjustmentRepository.findByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      val remand = adjustments.find { it.id.toString() == REMAND_ID }!!
      val taggedBail = adjustments.find { it.id.toString() == TAGGED_BAIL_ID }!!
      val unusedDeductions = adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }

      assertThat(remand.daysCalculated).isEqualTo(100)
      assertThat(remand.effectiveDays).isEqualTo(0)

      assertThat(taggedBail.days).isEqualTo(100)
      assertThat(taggedBail.effectiveDays).isEqualTo(50)

      assertThat(unusedDeductions).isNotNull
      assertThat(unusedDeductions!!.days).isEqualTo(150)

      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      assertThat(result).isNotNull
      assertThat(result!!.status).isEqualTo(UnusedDeductionsCalculationStatus.CALCULATED)

      val resultDto = webTestClient
        .get()
        .uri("/adjustments/person/$UNUSED_DEDUCTIONS_PRISONER_ID/unused-deductions-result")
        .headers(setAdjustmentsRWAuth())
        .exchange()
        .expectStatus().isOk
        .returnResult(UnusedDeductionsCalculationResultDto::class.java)
        .responseBody.blockFirst()!!

      assertThat(resultDto.status).isEqualTo(UnusedDeductionsCalculationStatus.CALCULATED)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/reset-data.sql",
    "classpath:test_data/insert-error-unused-deduction-adjustments.sql",
  )
  fun `handleAdjustmentEvent with an exception`() {
    CalculateReleaseDatesApiExtension.calculateReleaseDatesApi.stubCalculateUnusedDeductions()
    val eventType = "release-date-adjustments.adjustment.inserted"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(sentencingAdjustmentMessagePayload(REMAND_ID, UNUSED_DEDUCTIONS_ERROR_PRISONER_ID, eventType, "DPS"))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_ERROR_PRISONER_ID)
      assertThat(result).isNotNull
      assertThat(result!!.status).isEqualTo(UnusedDeductionsCalculationStatus.IN_PROGRESS)
    }

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
      assertThat(awsSqsUnusedDeductionsDlqClient!!.countAllMessagesOnQueue(unusedDeductionsDlqUrl!!).get()).isEqualTo(1)
      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(UNUSED_DEDUCTIONS_ERROR_PRISONER_ID)
      assertThat(result).isNull()
    }
  }

  private fun prisonerSearchPayload(offenderNumber: String, eventType: String): String? =
    """{"eventType":"$eventType", "additionalInformation": {"nomsNumber": "$offenderNumber", "categoriesChanged": ["SENTENCE"]}}"""
}
