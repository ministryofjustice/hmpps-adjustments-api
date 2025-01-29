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
  TCA, // time spent in custody abroad
  TSA, // time spent as an appeal applicant
}
