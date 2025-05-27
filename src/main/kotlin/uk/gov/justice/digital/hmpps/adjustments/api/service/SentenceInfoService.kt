package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient

@Service
class SentenceInfoService(private val remandAndSentencingApiClient: RemandAndSentencingApiClient) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun isRecall(sentenceCalculationType: String): Boolean {
    val sentenceDetails = remandAndSentencingApiClient.getSentenceTypeDetails(sentenceCalculationType)
    val isRecall = sentenceDetails?.recall?.type?.let { it != "NONE" && it.isNotEmpty() } ?: false
    log.info("Fetching recall type for nomisSentenceReference: $sentenceCalculationType is $isRecall")
    return isRecall
  }
}
