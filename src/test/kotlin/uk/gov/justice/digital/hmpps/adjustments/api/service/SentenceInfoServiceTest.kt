import org.hibernate.internal.util.collections.CollectionHelper.listOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.RecallType
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.SentenceDetailResponse
import uk.gov.justice.digital.hmpps.adjustments.api.service.SentenceInfoService
import java.time.LocalDateTime

class SentenceInfoServiceTest {

  private lateinit var remandAndSentencingApiClient: RemandAndSentencingApiClient
  private lateinit var sentenceInfoService: SentenceInfoService
  private val mockRecall = RecallType(type = "SOME_TYPE", isFixedTermRecall = true, isRecall = true, lengthInDays = 0) // Mock RecallType
  private val mockSentenceDetails = SentenceDetailResponse(
    nomisSentenceTypeReference = "type1",
    recall = mockRecall,
    nomisDescription = "description",
    isIndeterminate = false,
    nomisActive = true,
    nomisExpiryDate = null,
  )

  @BeforeEach
  fun setup() {
    remandAndSentencingApiClient = org.mockito.kotlin.mock<RemandAndSentencingApiClient>()
    sentenceInfoService = SentenceInfoService(remandAndSentencingApiClient)
  }

  @Test
  fun `should fetch data only once when cache is valid`() {
    // Arrange
    val mockResponse = listOf(mockSentenceDetails)
    `when`(remandAndSentencingApiClient.getSentenceTypesAndItsDetails()).thenReturn(mockResponse)

    // Act
    val firstCall = sentenceInfoService.isRecall("type1")
    val secondCall = sentenceInfoService.isRecall("type1")

    // Assert
    verify(remandAndSentencingApiClient, times(1)).getSentenceTypesAndItsDetails()
    assertTrue(firstCall)
    assertTrue(secondCall)
  }

  @Test
  fun `should fetch data again after cache expiration`() {
    // Arrange
    val mockResponse = listOf(mockSentenceDetails)
    `when`(remandAndSentencingApiClient.getSentenceTypesAndItsDetails()).thenReturn(mockResponse)

    // Act
    sentenceInfoService.isRecall("type1") // First call to populate cache
    sentenceInfoService.cacheExpiration = LocalDateTime.now().minusSeconds(1) // Simulate cache expiration
    sentenceInfoService.isRecall("type1") // Second call after expiration
    sentenceInfoService.isRecall("type1") // Third call after expiration (from cache)

    // Assert
    verify(remandAndSentencingApiClient, times(2)).getSentenceTypesAndItsDetails()
  }
}
