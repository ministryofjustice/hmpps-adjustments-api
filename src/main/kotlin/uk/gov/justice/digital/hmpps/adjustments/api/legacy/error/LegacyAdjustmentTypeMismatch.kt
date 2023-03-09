package uk.gov.justice.digital.hmpps.adjustments.api.legacy.error

class LegacyAdjustmentTypeMismatch(
  override var message: String,
) : Exception(message)
