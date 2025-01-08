package uk.gov.justice.digital.hmpps.adjustments.api.enums

enum class InterceptType(val message: String? = null) {
  NONE,
  FIRST_TIME_WITH_NO_ADJUDICATION("This service has identified an ADA adjustment with no supporting ADA adjudication. You must review the ADA adjustment and confirm whether it should be deleted."),
  FIRST_TIME("This service has identified ADA adjustments that were created in NOMIS. You must review the adjudications with ADAs and approve them in this service."),
  UPDATE("Updates have been made to ADA (Additional days awarded) information, which need to be approved."),
  PADAS("There are PADAs (Prospective additional days awarded) recorded for %s. Review the PADAs and approve the ones that are relevant to the current sentence."),
  PADA("There is a PADA (Prospective additional days awarded) recorded for %s. Review the PADA and approve if it's relevant to the current sentence."),
}
