package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

data class SentenceInfo(
  val sentenceSequence: Int,
  val recall: Boolean,
) {
  constructor(sentence: SentenceAndOffences, remandAndSentencingApiClient: RemandAndSentencingApiClient) : this(
    sentence.sentenceSequence,
    isRecall(sentence, remandAndSentencingApiClient),
  )

  companion object {
    fun isRecall(sentence: SentenceAndOffences, remandAndSentencingApiClient: RemandAndSentencingApiClient): Boolean {
      val sentenceDetails = remandAndSentencingApiClient.getSentenceTypeDetails(sentence.sentenceCalculationType)
      return sentenceDetails?.recall?.type?.let { it != "NONE" && it.isNotEmpty() } ?: false
    }
  }
}
