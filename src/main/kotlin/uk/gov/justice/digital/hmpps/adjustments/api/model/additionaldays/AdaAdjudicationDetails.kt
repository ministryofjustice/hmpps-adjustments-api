package uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays

import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType
import java.time.LocalDate

data class AdaAdjudicationDetails(
  val awarded: List<AdasByDateCharged> = listOf(),
  val totalAwarded: Int = 0,
  val suspended: List<AdasByDateCharged> = listOf(),
  val totalSuspended: Int = 0,
  val quashed: List<AdasByDateCharged> = listOf(),
  val totalQuashed: Int = 0,
  val awaitingApproval: List<AdasByDateCharged> = listOf(),
  val totalAwaitingApproval: Int = 0,
  val prospective: List<AdasByDateCharged> = listOf(),
  val totalProspective: Int = 0,
  val intercept: AdaIntercept = AdaIntercept(InterceptType.NONE, 0, false),
  val totalExistingAdas: Int = 0,
  val showExistingAdaMessage: Boolean = false,
  val recallWithMissingOutcome: Boolean = false,
  val earliestNonRecallSentenceDate: LocalDate? = null,
  val earliestRecallDate: LocalDate? = null,
)
