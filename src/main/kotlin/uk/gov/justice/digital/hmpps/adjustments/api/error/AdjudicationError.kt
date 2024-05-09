package uk.gov.justice.digital.hmpps.adjustments.api.error

class AdjudicationError(
  override var message: String,
) : Exception(message)
