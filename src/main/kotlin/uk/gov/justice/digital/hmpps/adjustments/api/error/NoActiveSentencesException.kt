package uk.gov.justice.digital.hmpps.adjustments.api.error

class NoActiveSentencesException(
  override var message: String,
) : Exception(message)
