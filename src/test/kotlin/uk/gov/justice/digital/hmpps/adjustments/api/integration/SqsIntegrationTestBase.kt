package uk.gov.justice.digital.hmpps.adjustments.api.integration

import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SqsIntegrationTestBase : IntegrationTestBase() {

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

  fun HmppsSqsProperties.domaineventsTopicConfig() =
    topics["domainevents"] ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  @BeforeEach
  fun cleanQueue() {
    adjustmentsQueue.sqsClient.purgeQueue(PurgeQueueRequest(adjustmentsQueue.queueUrl))
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
    val queueAttributes = adjustmentsQueue.sqsClient.getQueueAttributes(adjustmentsQueue.queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun getLatestMessage(): ReceiveMessageResult? {
    return adjustmentsQueue.sqsClient.receiveMessage(adjustmentsQueue.queueUrl)
  }
}
