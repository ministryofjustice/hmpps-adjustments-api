package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences

data class SentenceInfo(
  val sentenceSequence: Int,
  val recall: Boolean,
) {
  constructor(sentence: SentenceAndOffences) : this(sentence.sentenceSequence, isRecall(sentence))

  companion object {
    fun isRecall(sentence: SentenceAndOffences): Boolean = recallTypes.contains(sentence.sentenceCalculationType)

    private val recallTypes = listOf(
      "LR",
      "LR_ORA",
      "LR_YOI_ORA",
      "LR_SEC91_ORA",
      "LRSEC250_ORA",
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
      "FTR_HDC_ORA",
      "CUR",
      "CUR_ORA",
      "HDR",
      "HDR_ORA",
      "FTR_HDC",
      "LR_DPP",
      "LR_DLP",
      "LR_ALP",
      "LR_ALP_LASPO",
      "LR_ALP_CDE18",
      "LR_ALP_CDE21",
      "LR_LIFE",
      "LR_EPP",
      "LR_IPP",
      "LR_MLP",
      "LR_ES"
    )
  }
}
