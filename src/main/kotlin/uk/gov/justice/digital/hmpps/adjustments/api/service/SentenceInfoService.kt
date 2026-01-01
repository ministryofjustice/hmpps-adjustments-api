package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing.SentenceDetailResponse
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

@Service
class SentenceInfoService(private val remandAndSentencingApiClient: RemandAndSentencingApiClient) {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val cache = ConcurrentHashMap<String, Any>()
  var cacheExpiration: LocalDateTime = LocalDateTime.now()

  fun isRecall(sentenceCalculationType: String): Boolean {
    val sentenceDetails = getCachedSentenceDetails()
      ?.find { it.nomisSentenceTypeReference == sentenceCalculationType }
    val isRecall = sentenceDetails?.recall?.type?.let { it != "NONE" && it.isNotEmpty() } ?: false
    log.info("Fetching recall type for nomisSentenceReference: $sentenceCalculationType is $isRecall")
    return isRecall
  }

  private fun getCachedSentenceDetails(): List<SentenceDetailResponse>? {
    val now = LocalDateTime.now()
    if (cache.isEmpty() || now.isAfter(cacheExpiration)) {
      log.info("Cache is empty or expired. Fetching new data.")
      val sentenceDetails = remandAndSentencingApiClient.getSentenceTypesAndItsDetails()
      if (sentenceDetails != null) {
        cache.clear()
        cache.put("sentenceDetails", sentenceDetails)
        cacheExpiration = LocalDateTime.now().with(LocalTime.MAX) // Set expiration to midnight
      }
    }
    @Suppress("UNCHECKED_CAST")
    return cache.get("sentenceDetails") as? List<SentenceDetailResponse>
  }
}
