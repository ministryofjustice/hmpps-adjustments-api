package uk.gov.justice.digital.hmpps.adjustments.api.enums

enum class InterceptType(val message: String? = null) {
  NONE,
  FIRST_TIME("The first time you use the adjustments service, you need to check if the existing adjustment information from NOMIS is correct."),
  UPDATE("Updates have been made to %s's adjustment information, which need to be approved."),
  PADA("There is a prospective ADA recorded for %s"),
}
