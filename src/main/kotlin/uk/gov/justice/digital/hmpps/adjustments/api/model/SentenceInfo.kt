package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

data class SentenceInfo(
  val sentenceSequence: Int,
  val recall: Boolean,
) {
  constructor(sentence: SentenceAndOffences, recall: Boolean) : this(sentence.sentenceSequence, recall)
}
