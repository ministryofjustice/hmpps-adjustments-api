package uk.gov.justice.digital.hmpps.adjustments.api.error

class ApiValidationException(
  override var message: String,
) : Exception(message)
