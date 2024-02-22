package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.ADDITIONAL_DAYS_AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Sanction
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdaStatus.AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdaStatus.PENDING_APPROVAL
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.AWARDED_OR_PENDING
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.PROSPECTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.QUASHED
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.SUSPENDED
import uk.gov.justice.digital.hmpps.adjustments.api.service.InterceptType.FIRST_TIME
import uk.gov.justice.digital.hmpps.adjustments.api.service.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.service.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.service.InterceptType.UPDATE
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ChargeStatus { AWARDED_OR_PENDING, SUSPENDED, QUASHED, PROSPECTIVE }

enum class AdaStatus(alternativeName: String? = null) { AWARDED, PENDING_APPROVAL("PENDING APPROVAL"), SUSPENDED, QUASHED, PROSPECTIVE }

data class AdasByDateCharged(
  val dateChargeProved: LocalDate,
  val charges: MutableList<Ada>,
  val total: Int? = null,
  val status: AdaStatus? = null,
  val adjustmentId: UUID? = null,
)

data class Ada(
  val dateChargeProved: LocalDate,
  val chargeNumber: Long,
  val toBeServed: String? = null,
  val heardAt: String? = null,
  val status: ChargeStatus,
  val days: Int,
  val sequence: Long? = null,
  val consecutiveToSequence: Long? = null,
)

enum class InterceptType { NONE, FIRST_TIME, UPDATE, PADA }

data class AdaIntercept(
  val type: InterceptType,
  val number: Int,
  val anyProspective: Boolean,
)

@Service
@Transactional(readOnly = true)
class AdditionalDaysAwardedService(
  private val prisonService: PrisonService,
  private val adjustmentRepository: AdjustmentRepository,
  private val prisonApiClient: PrisonApiClient,
) {
  fun shouldIntercept(nomsId: String): AdaIntercept {
    val adaAdjustments = adjustmentRepository.findByPersonAndAdjustmentType(nomsId, ADDITIONAL_DAYS_AWARDED)
    val anyUnlinkedAdas =
      adaAdjustments.any { it.additionalDaysAwarded?.adjudicationCharges?.isEmpty() ?: false && it.effectiveDays > 0 }

    val adas = lookupAdas(nomsId)
    val awardedOrPending = this.getAdasByDateCharged(adas, AWARDED_OR_PENDING)
    val (awarded, pendingApproval) = filterAdasByMatchingAdjustment(awardedOrPending, adaAdjustments)

    val allProspective = this.getAdasByDateCharged(adas, PROSPECTIVE)
    val (prospectiveAwarded, prospective) = filterAdasByMatchingAdjustment(allProspective, adaAdjustments)

    val allAwarded = awarded.plus(prospectiveAwarded)

    val allQuashed = this.getAdasByDateCharged(adas, QUASHED)
    val quashed = filterQuashedAdasByMatchingChargeIds(allQuashed, adaAdjustments)

    return deriveAdaIntercept(anyUnlinkedAdas, prospective, pendingApproval, quashed, adaAdjustments, allAwarded)
  }

  private fun deriveAdaIntercept(
    anyUnlinkedAdas: Boolean,
    prospective: List<AdasByDateCharged>,
    pendingApproval: List<AdasByDateCharged>,
    quashed: List<AdasByDateCharged>,
    adaAdjustments: List<Adjustment>,
    allAwarded: List<AdasByDateCharged>,
  ): AdaIntercept {
    if (anyUnlinkedAdas) {
      return AdaIntercept(
        type = FIRST_TIME,
        number = prospective.size + pendingApproval.size,
        anyProspective = prospective.isNotEmpty(),
      )
    }

    if (pendingApproval.isNotEmpty()) {
      return AdaIntercept(
        type = UPDATE,
        number = pendingApproval.size,
        anyProspective = prospective.isNotEmpty(),
      )
    }


    if (quashed.isNotEmpty()) {
      return AdaIntercept(
        type = UPDATE,
        number = quashed.size,
        anyProspective = prospective.isNotEmpty(),
      )
    }

    val totalAdjustments = adaAdjustments.sumOf { it.days ?: 0 }
    val totalAdjudications = allAwarded.sumOf { it.total ?: 0 }

    if (totalAdjustments != totalAdjudications) {
      return AdaIntercept(
        type = UPDATE,
        number = allAwarded.size,
        anyProspective = prospective.isNotEmpty(),
      )
    }


    if (prospective.isNotEmpty()) {
      //      TODO the following logic is in the UI and is UI specific.. how do we simulate the same behaviour in the api? Do we need to?
      //      if (req != null) {
      //        val lastApproved = additionalDaysAwardedStoreService.getLastApprovedDate(req, nomsId)
      //        if (lastApproved != null && dayjs(lastApproved).add(1, "hour").isAfter(dayjs())) {
      //          return AdaIntercept(type = "NONE", number = 0, anyProspective = false)
      //        }
      //      }
      return AdaIntercept(
        type = PADA,
        number = prospective.size,
        anyProspective = true,
      )
    }

    return AdaIntercept(
      type = NONE,
      number = 0,
      anyProspective = false,
    )
  }

  private fun filterQuashedAdasByMatchingChargeIds(
    adas: List<AdasByDateCharged>,
    adjustments: List<Adjustment>,
  ): List<AdasByDateCharged> {
    val chargeIds = adjustments
      .filter { it.additionalDaysAwarded != null }
      .flatMap { it.additionalDaysAwarded!!.adjudicationCharges.map { charge -> charge.adjudicationId } }

    return adas
      .filter { adaByDate ->
        adaByDate.charges.any { charge -> chargeIds.contains(charge.chargeNumber) }
      }.map { it.copy(status = PENDING_APPROVAL) }
  }

  data class AwardedAndPending(
    val awarded: List<AdasByDateCharged> = emptyList(),
    val pendingApproval: List<AdasByDateCharged> = emptyList(),
  )

  private fun filterAdasByMatchingAdjustment(
    adas: List<AdasByDateCharged>,
    adjustments: List<Adjustment>,
  ): AwardedAndPending {
    if (adjustments.any { it.additionalDaysAwarded != null }) {
      return AwardedAndPending(pendingApproval = adas.map { it.copy(status = PENDING_APPROVAL) })
    }

    val awardedAndPendingAdas = adas.fold(mutableListOf<AdasByDateCharged>()) { acc, cur ->
      if (adjustments.any { adjustmentMatchesAdjudication(cur, it) }) {
        val adjustment = adjustments.first { adjustmentMatchesAdjudication(cur, it) }
        acc.add(cur.copy(status = AWARDED, adjustmentId = adjustment.id))
      } else {
        acc.add(cur.copy(status = PENDING_APPROVAL))
      }
      acc
    }

    return AwardedAndPending(
      awarded = awardedAndPendingAdas.filter { it.status == AWARDED },
      pendingApproval = awardedAndPendingAdas.filter { it.status == PENDING_APPROVAL },
    )
  }

  private fun adjustmentMatchesAdjudication(adjudication: AdasByDateCharged, adjustment: Adjustment): Boolean {
    return (
      adjudication.total == adjustment.days &&
        adjudication.dateChargeProved == adjustment.fromDate &&
        adjustment.additionalDaysAwarded != null &&
        adjudication.charges.map { it.chargeNumber }
          .toSet() == adjustment.additionalDaysAwarded!!.adjudicationCharges.map { it.adjudicationId }.toSet()
      )
  }

  private fun lookupAdas(nomsId: String): List<Ada> {
    val startOfSentenceEnvelope = prisonService.getStartOfSentenceEnvelopeExcludingRecalls(nomsId) ?: return emptyList()
    val adjudications = prisonApiClient.getAdjudications(nomsId)
    val individualAdjudications =
      adjudications.results.map { prisonApiClient.getAdjudication(nomsId, it.adjudicationNumber) }
    return getAdas(individualAdjudications, startOfSentenceEnvelope)
  }

  private fun getAdasByDateCharged(adas: List<Ada>, filterStatus: ChargeStatus): List<AdasByDateCharged> {

    val adasByDateCharged = adas.filter { it.status == filterStatus }.groupBy { it.dateChargeProved }
    // TODO did not sort - think unnecessary
    // return  adas.filter { it.status == filterStatus }.groupBy { it.dateChargeProved }.toSortedMap()
    // TODO did not return associateConsecutiveAdas - think unnecessary
    return associateConsecutiveAdas(adasByDateCharged, adas).map {
      it.copy(
        total = calculateTotal(it),
        status = if (filterStatus != AWARDED_OR_PENDING) AdaStatus.valueOf(filterStatus.name) else null,
      )
    }
  }

  private fun calculateTotal(adaByDateCharge: AdasByDateCharged): Int {
    if (adaByDateCharge.charges.size == 1) {
      return adaByDateCharge.charges[0].days
    }
    val baseCharges = adaByDateCharge.charges.filter { it.consecutiveToSequence == null }
    val consecCharges = adaByDateCharge.charges.filter { it.consecutiveToSequence != null }

    val chains: MutableList<MutableList<Ada>> = mutableListOf()

    baseCharges.forEach {
      val chain = mutableListOf(it)
      chains.add(chain)
      createChain(it, chain, consecCharges)
    }

    val calculatedDays = chains
      .filter { it.isNotEmpty() }
      .map { chain -> chain.sumOf { it.days } }
    if (calculatedDays.isEmpty()) {
      return 0
    }
    return calculatedDays.maxOrNull() ?: 0
  }

  private fun createChain(ada: Ada, chain: MutableList<Ada>, consecCharges: List<Ada>) {
    val consecFrom = consecCharges.find { it.consecutiveToSequence == ada.sequence }
    consecFrom?.let {
      chain.add(it)
      createChain(it, chain, consecCharges)
    }
  }

  /*
   * Sets the toBeServed of the groupedAdas for the review screen, can be either Consecutive, Concurrent or Forthwith
   */
  private fun associateConsecutiveAdas(
    adasByDateCharged: Map<LocalDate, List<Ada>>,
    adas: List<Ada>,
  ): List<AdasByDateCharged> {
    val consecutiveSourceAdas = getSourceAdaForConsecutive(adas)
    return adasByDateCharged.map {
      val charges = it.value
      if (charges.size == 1) {
        AdasByDateCharged(dateChargeProved = it.key, charges = mutableListOf(charges[0].copy(toBeServed = "Forthwith")))
      } else {
        // Label consecutive or concurrent adas
        val consecutiveAndConcurrentCharges = charges.map { charge ->
          if (validConsecutiveSequence(charge, consecutiveSourceAdas)) {
            val consecutiveAda = consecutiveSourceAdas.first { c -> adaHasSequence(charge.consecutiveToSequence!!, c) }
            charge.copy(toBeServed = "Consecutive to ${consecutiveAda.chargeNumber}")
          } else if (
            !validConsecutiveSequence(charge, consecutiveSourceAdas) &&
            !isSourceForConsecutiveChain(consecutiveSourceAdas, charge)
          ) {
            charge.copy(toBeServed = "Concurrent")
          } else {
            charge.copy(toBeServed = "Forthwith")
          }
        }
        // TODO In the UI this is sorted - but not necessary for intercept logic. The other functions from the UI that use this method will also be changed to use the api at which point this will need to be sorted
        AdasByDateCharged(dateChargeProved = it.key, charges = consecutiveAndConcurrentCharges.toMutableList())
      }
    }
  }

  private fun isSourceForConsecutiveChain(consecutiveSourceAdas: List<Ada>, charge: Ada) =
    consecutiveSourceAdas.any { consecutiveAda -> adaHasSequence(charge.sequence, consecutiveAda) }

  private fun validConsecutiveSequence(charge: Ada, consecutiveSourceAdas: List<Ada>): Boolean =
    charge.consecutiveToSequence != null &&
      consecutiveSourceAdas.any { consecutiveAda -> adaHasSequence(charge.consecutiveToSequence, consecutiveAda) }

  private fun getSourceAdaForConsecutive(allAdas: List<Ada>): List<Ada> =
    allAdas.filter { ada -> ada.consecutiveToSequence != null && allAdas.any { it.sequence == ada.consecutiveToSequence } }
      .map { consecutiveAda -> allAdas.first { sourceAda -> sourceAda.sequence == consecutiveAda.sequence } }

  private fun prospectiveOrSanctioned(hearing: Hearing, startOfSentenceEnvelope: LocalDate): Boolean {
    return hearing.results != null && hearing.results.any { r ->
      r.sanctions != null && r.sanctions.any { s ->
        isProspectiveOrSanctioned(s, hearing.hearingTime, startOfSentenceEnvelope)
      }
    }
  }

  private fun getAdas(
    individualAdjudications: List<AdjudicationDetail>,
    startOfSentenceEnvelope: LocalDate,
  ): List<Ada> {
    val adasToTransform = individualAdjudications.filter { ad ->
      ad.hearings != null
        && ad.hearings.any { h ->
        prospectiveOrSanctioned(h, startOfSentenceEnvelope)

      }
    }

    return adasToTransform.fold(mutableListOf()) { acc, cur ->
      cur.hearings!!.filter { h ->
        h.results != null && h.results.any { r ->
          r.sanctions != null && r.sanctions.any { s ->
            isProspectiveOrSanctioned(s, h.hearingTime, startOfSentenceEnvelope)
          }
        }
      }.forEach { h ->
        val result = h.results!!.first { r ->
          r.sanctions != null && r.sanctions.any { s ->
            isProspectiveOrSanctioned(s, h.hearingTime, startOfSentenceEnvelope)
          }
        }
        result.sanctions!!.filter { s -> isProspectiveOrSanctioned(s, h.hearingTime, startOfSentenceEnvelope) }
          .forEach { sanction ->
            acc.add(
              Ada(
                dateChargeProved = h.hearingTime.toLocalDate(),
                days = sanction.sanctionDays,
                chargeNumber = cur.adjudicationNumber,
                consecutiveToSequence = sanction.consecutiveSanctionSeq,
                heardAt = h.establishment,
                sequence = sanction.sanctionSeq,
                status = deriveChargeStatus(sanction),
              ),
            )
          }

      }
      acc
    }
  }

  private fun deriveChargeStatus(sanction: Sanction): ChargeStatus {
    if (isSuspended(sanction)) return SUSPENDED
    if (sanction.status == "Quashed") return QUASHED
    if (isProspectiveAda(sanction)) return PROSPECTIVE
    return AWARDED_OR_PENDING
  }

  private fun isProspectiveOrSanctioned(
    sanction: Sanction,
    hearingTime: LocalDateTime,
    startOfSentenceEnvelope: LocalDate,
  ) = isProspectiveAda(sanction) || isSanctionedAda(sanction, hearingTime.toLocalDate(), startOfSentenceEnvelope)

  /* The adjudications status from NOMIS DB mapped to the adjudications API status are listed here temporarily to make it easier to implement the stories which use the NOMIS status
   * 'AS_AWARDED' = 'Activated as Awarded'
   * 'AWARD_RED' = 'Activated with Quantum Reduced'
   * 'IMMEDIATE' = 'Immediate'
   * 'PROSPECTIVE' = 'Prospective'
   * 'QUASHED' = 'Quashed'
   * 'REDAPP' = 'Reduced on Appeal'
   * 'SUSPENDED' = 'Suspended'
   * 'SUSPEN_EXT' = 'Period of Suspension Extended'
   * 'SUSPEN_RED' = 'Period of Suspension Shortened
   * 'SUSP_PROSP' = 'Suspended and Prospective'
   */
  private fun sanctionIsProspective(s: Sanction) = s.status == "Prospective" || s.status == "Suspended and Prospective"

  private fun sanctionIsAda(s: Sanction) = s.sanctionType === "Additional Days Added"
  private fun isSanctionedAda(sanction: Sanction, hearingDate: LocalDate, startOfSentenceEnvelope: LocalDate) =
    sanctionIsAda(sanction) &&
      !sanctionIsProspective(sanction) &&
      sanction.sanctionDays > 0 && hearingDate >= startOfSentenceEnvelope

  private fun isProspectiveAda(s: Sanction) = sanctionIsAda(s) && sanctionIsProspective(s)

  private fun adaHasSequence(sequence: Long?, ada: Ada) = sequence != null && sequence == ada.sequence

  private fun isSuspended(sanction: Sanction): Boolean = sanction.status == "Suspended" ||
    sanction.status == "Suspended and Prospective" ||
    sanction.status == "Period of Suspension Extended" ||
    sanction.status == "Period of Suspension Shortened"
}
