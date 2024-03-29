package uk.gov.justice.digital.hmpps.adjustments.api.legacy.model

enum class LegacyAdjustmentType {
  ADA,
  RADA,
  UAL,
  LAL,
  SREM, // special remission
  RSR,
  RST,
  RX, // remand
  S240A, // tagged bail
  UR, // unused remand
}
