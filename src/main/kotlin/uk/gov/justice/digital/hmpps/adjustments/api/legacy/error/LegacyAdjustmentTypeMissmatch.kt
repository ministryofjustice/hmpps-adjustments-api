package uk.gov.justice.digital.hmpps.adjustments.api.legacy.error

class LegacyAdjustmentTypeMissmatch(
  override var message: String,
) : Exception(message)
