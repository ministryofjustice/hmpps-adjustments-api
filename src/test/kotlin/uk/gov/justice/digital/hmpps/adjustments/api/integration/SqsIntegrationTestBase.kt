package uk.gov.justice.digital.hmpps.adjustments.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.core.ConditionFactory
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SqsIntegrationTestBase : IntegrationTestBase() {
  protected val awaitAtMost30Secs: ConditionFactory get() = await.atMost(Duration.ofSeconds(30))

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @SpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  private val domainEventsTopic by lazy { hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found") }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val adjustmentsQueue by lazy { hmppsQueueService.findByQueueId("adjustments") as HmppsQueue }

  protected val prisonerListenerQueue by lazy { hmppsQueueService.findByQueueId("prisonerlistener") as HmppsQueue }
  internal val prisonerListenerQueueUrl by lazy { prisonerListenerQueue.queueUrl }
  internal val unusedDeductionsQueue by lazy { hmppsQueueService.findByQueueId("unuseddeductions") as HmppsQueue }

  internal val awsSqsUnusedDeductionsClient by lazy { unusedDeductionsQueue.sqsClient }
  internal val awsSqsUnusedDeductionsDlqClient by lazy { unusedDeductionsQueue.sqsDlqClient }
  internal val unusedDeductionsQueueUrl by lazy { unusedDeductionsQueue.queueUrl }
  internal val unusedDeductionsDlqUrl by lazy { unusedDeductionsQueue.dlqUrl }


  fun HmppsSqsProperties.domaineventsTopicConfig() =
    topics["domainevents"] ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  @BeforeEach
  fun cleanQueue() {
    await untilCallTo {
      adjustmentsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(adjustmentsQueue.queueUrl).build())
      adjustmentsQueue.sqsClient.countMessagesOnQueue(adjustmentsQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      awsSqsUnusedDeductionsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(unusedDeductionsQueue.queueUrl).build())
      unusedDeductionsQueue.sqsClient.countMessagesOnQueue(unusedDeductionsQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      awsSqsUnusedDeductionsDlqClient?.purgeQueue(PurgeQueueRequest.builder().queueUrl(unusedDeductionsQueue.dlqUrl).build())
      unusedDeductionsQueue.sqsDlqClient!!.countMessagesOnQueue(unusedDeductionsQueue.dlqUrl!!).get()
    } matches { it == 0 }
  }

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { LocalStackContainer.setLocalStackProperties(it, registry) }
    }
  }

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    return adjustmentsQueue.sqsClient.countMessagesOnQueue(adjustmentsQueue.queueUrl).get()
  }

  fun getLatestMessage(): ReceiveMessageResponse? {
    return adjustmentsQueue.sqsClient.receiveMessage(ReceiveMessageRequest.builder().maxNumberOfMessages(2).queueUrl(adjustmentsQueue.queueUrl).build()).get()
  }
}
