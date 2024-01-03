package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

data class SentenceInfo(
  val sentenceSequence: Int,
  val recall: Boolean,
) {
  constructor(sentence: SentenceAndOffences) : this(sentence.sentenceSequence, isRecall(sentence))

  companion object {
    fun isRecall(sentence: SentenceAndOffences): Boolean {
      return recallTypes.contains(sentence.sentenceCalculationType)
    }

    private val recallTypes = listOf(
      "LR_EDS18",
      "LR_EDS21",
      "LR_EDSU18",
      "LR_LASPO_AR",
      "LR_LASPO_DR",
      "LR_SEC236A",
      "LR_SOPC18",
      "LR_SOPC21",
      "14FTR_ORA",
      "FTR",
      "FTR_ORA",
      "FTR_SCH15",
      "FTRSCH15_ORA",
      "FTRSCH18",
      "FTRSCH18_ORA",
      "14FTRHDC_ORA",
    )
  }
}
