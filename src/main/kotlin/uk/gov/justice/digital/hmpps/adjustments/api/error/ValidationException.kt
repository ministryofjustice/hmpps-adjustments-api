package uk.gov.justice.digital.hmpps.adjustments.api.error

class ValidationException(
  override var message: String,
) : Exception(message)
