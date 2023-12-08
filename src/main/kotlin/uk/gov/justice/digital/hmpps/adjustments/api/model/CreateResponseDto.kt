package uk.gov.justice.digital.hmpps.adjustments.api.model

import java.util.UUID

data class CreateResponseDto(val adjustmentIds: List<UUID>)
