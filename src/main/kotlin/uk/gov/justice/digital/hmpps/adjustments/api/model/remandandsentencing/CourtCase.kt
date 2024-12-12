package uk.gov.justice.digital.hmpps.adjustments.api.model.remandandsentencing

import java.util.UUID

data class CourtCase(val courtCaseUuid: UUID, val prisonerId: String)
