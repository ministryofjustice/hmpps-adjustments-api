package uk.gov.justice.digital.hmpps.adjustments.api.enums

enum class AdaStatus(val alternativeName: String? = null) { AWARDED, PENDING_APPROVAL("PENDING APPROVAL"), SUSPENDED, QUASHED, PROSPECTIVE }
