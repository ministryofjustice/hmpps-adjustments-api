package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDate

data class PrisonApiCharge(
  val chargeId: Long = -1,
  val offenceCode: String = "",
  val offenceStatue: String = "",
  val offenceDate: LocalDate? = null,
  val offenceEndDate: LocalDate? = null,
  val offenceDescription: String = "",
  val guilty: Boolean = false,
  val courtCaseId: Long = -1,
  val courtCaseRef: String? = null,
  val courtLocation: String? = null,
  val sentenceSequence: Int? = null,
  val sentenceDate: LocalDate? = null,
  val resultDescription: String? = null,
)
