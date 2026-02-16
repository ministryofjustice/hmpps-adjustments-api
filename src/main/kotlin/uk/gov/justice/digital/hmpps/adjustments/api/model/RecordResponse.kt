package uk.gov.justice.digital.hmpps.adjustments.api.model

data class RecordResponse<T>(
  val record: T,
  val adjustmentEventToEmit: AdjustmentEventMetadata,
)
