package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.ADDITIONAL_DAYS_AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ProspectiveAdaRejection
import uk.gov.justice.digital.hmpps.adjustments.api.enums.AdaStatus
import uk.gov.justice.digital.hmpps.adjustments.api.enums.AdaStatus.AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.enums.AdaStatus.PENDING_APPROVAL
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus.AWARDED_OR_PENDING
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus.PROSPECTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus.QUASHED
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus.SUSPENDED
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.FIRST_TIME
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.UPDATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ProspectiveAdaRejectionDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.Ada
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdasByDateCharged
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Sanction
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.ProspectiveAdaRejectionRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdditionalDaysAwardedService(
  private val prisonService: PrisonService,
  private val adjustmentRepository: AdjustmentRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prospectiveAdaRejectionRepository: ProspectiveAdaRejectionRepository,
) {

  @Transactional
  fun rejectProspectiveAda(prospectiveAdaRejectionDto: ProspectiveAdaRejectionDto) {
    prospectiveAdaRejectionRepository.save(ProspectiveAdaRejection(person = prospectiveAdaRejectionDto.person, dateChargeProved = prospectiveAdaRejectionDto.dateChargeProved, days = prospectiveAdaRejectionDto.days))
  }

  fun getAdaAdjudicationDetails(nomsId: String, selectedProspectiveAdaDates: List<String> = listOf()): AdaAdjudicationDetails {
    val sentences = prisonService.getActiveSentencesExcludingRecalls(nomsId)
    if (sentences.isEmpty()) {
      return AdaAdjudicationDetails()
    }
    val latestSentenceDate = sentences.maxOf { it.sentenceDate }
    val startOfSentenceEnvelope = sentences.minOf { it.sentenceDate }
    val adaAdjustments = adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(nomsId, ADDITIONAL_DAYS_AWARDED)
    val adas = lookupAdas(nomsId, startOfSentenceEnvelope)

    var (awarded, pendingApproval) = filterAdasByMatchingAdjustment(
      getAdasByDateCharged(adas, AWARDED_OR_PENDING),
      adaAdjustments,
    )

    val suspended = getAdasByDateCharged(adas, SUSPENDED)
    val totalSuspended = getTotalDays(suspended)

    val (prospectiveAwarded, prospective) = filterAdasByMatchingAdjustment(
      getAdasByDateCharged(adas, PROSPECTIVE),
      adaAdjustments,
    )
    awarded = awarded + prospectiveAwarded
    val totalAwarded = getTotalDays(awarded)

    val totalProspective = getTotalDays(prospective)

    val selectedProspectiveAdas = prospective.filter {
      selectedProspectiveAdaDates.contains(it.dateChargeProved.toString())
    }
    pendingApproval = pendingApproval + selectedProspectiveAdas
    val totalAwaitingApproval = getTotalDays(pendingApproval)

    val quashed = filterQuashedAdasByMatchingChargeIds(getAdasByDateCharged(adas, QUASHED), adaAdjustments)
    val totalQuashed = getTotalDays(quashed)

    val totalExistingAdas = adaAdjustments.map { it.effectiveDays }.reduceOrNull { acc, it -> acc + it } ?: 0

    val padaRejections = prospectiveAdaRejectionRepository.findByPerson(nomsId)

    return AdaAdjudicationDetails(
      awarded,
      totalAwarded,
      suspended,
      totalSuspended,
      quashed,
      totalQuashed,
      pendingApproval,
      totalAwaitingApproval,
      prospective,
      totalProspective,
      intercept = deriveAdaIntercept(
        nomsId,
        prospective,
        pendingApproval,
        quashed,
        adaAdjustments,
        awarded,
        latestSentenceDate,
        padaRejections,
      ),
      totalExistingAdas,
      pendingApproval.isEmpty() && quashed.isEmpty() && awarded.isEmpty(),
    )
  }

  private fun getTotalDays(adas: List<AdasByDateCharged>): Int {
    return adas.map { it.total!! }.reduceOrNull { acc, it -> acc + it } ?: 0
  }

  private fun getMessageParams(nomsId: String): List<String> {
    val prisonerDetail = prisonApiClient.getPrisonerDetail(nomsId)
    return listOf("${prisonerDetail.lastName}, ${prisonerDetail.firstName}".toTitleCase())
  }

  fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { it ->
      it.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

  private fun deriveAdaIntercept(
    nomsId: String,
    prospective: List<AdasByDateCharged>,
    pendingApproval: List<AdasByDateCharged>,
    quashed: List<AdasByDateCharged>,
    adaAdjustments: List<Adjustment>,
    allAwarded: List<AdasByDateCharged>,
    latestSentenceDate: LocalDate,
    padaRejections: List<ProspectiveAdaRejection>,
  ): AdaIntercept {
    val anyUnlinkedAdas = anyUnlinkedAdas(adaAdjustments)
    val totalAdjustments = adaAdjustments.sumOf { it.effectiveDays }
    val totalAdjudications = allAwarded.sumOf { it.total ?: 0 }
    val numPendingApproval = pendingApproval.size
    val numQuashed = quashed.size
    val nonRejectedProspective = prospective.filter {
      val rejections = padaRejections.filter { reject -> reject.dateChargeProved == it.dateChargeProved && reject.days == it.total }
      val latestRejection = rejections.maxByOrNull { it.rejectionAt }
      latestRejection == null || latestRejection.rejectionAt.isBefore(latestSentenceDate.atStartOfDay())
    }
    val numProspective = nonRejectedProspective.size
    val anyProspective = numProspective > 0
    return when {
      anyUnlinkedAdas -> AdaIntercept(FIRST_TIME, numProspective + numPendingApproval, anyProspective)
      numPendingApproval > 0 -> AdaIntercept(UPDATE, numPendingApproval, anyProspective, getMessageParams(nomsId))
      numQuashed > 0 -> AdaIntercept(UPDATE, numQuashed, anyProspective, getMessageParams(nomsId))
      totalAdjustments != totalAdjudications -> AdaIntercept(
        UPDATE,
        allAwarded.size,
        anyProspective,
        getMessageParams(nomsId),
      )

      numProspective > 0 -> AdaIntercept(PADA, numProspective, true, getMessageParams(nomsId))
      else -> AdaIntercept(NONE, 0, false)
    }
  }

  private fun anyUnlinkedAdas(adaAdjustments: List<Adjustment>): Boolean {
    return adaAdjustments.any { it.additionalDaysAwarded?.adjudicationCharges?.isEmpty() ?: true && it.effectiveDays > 0 }
  }

  private fun filterQuashedAdasByMatchingChargeIds(
    adas: List<AdasByDateCharged>,
    adjustments: List<Adjustment>,
  ): List<AdasByDateCharged> {
    val chargeIds = adjustments.filter { it.additionalDaysAwarded != null }
      .flatMap { it.additionalDaysAwarded!!.adjudicationCharges.map { charge -> charge.adjudicationId } }
    return adas.filter { adaByDate -> adaByDate.charges.any { charge -> chargeIds.contains(charge.chargeNumber) } }
      .map { it.copy(status = PENDING_APPROVAL) }
  }

  private fun filterAdasByMatchingAdjustment(
    adas: List<AdasByDateCharged>,
    adjustments: List<Adjustment>,
  ): Pair<List<AdasByDateCharged>, List<AdasByDateCharged>> {
    return if (anyUnlinkedAdas(adjustments)) {
      // An ADA has been created in NOMIS, Revert everything to pending approval
      Pair(
        emptyList(),
        adas.map { it.copy(status = PENDING_APPROVAL) },
      )
    } else {
      val awardedAndPendingAdas = adas.map { adasByDate ->
        if (adjustments.any { adjustmentMatchesAdjudication(adasByDate, it) }) {
          val adjustment = adjustments.first { adjustmentMatchesAdjudication(adasByDate, it) }
          adasByDate.copy(status = AWARDED, adjustmentId = adjustment.id)
        } else {
          adasByDate.copy(status = PENDING_APPROVAL)
        }
      }
      Pair(
        awardedAndPendingAdas.filter { it.status == AWARDED },
        awardedAndPendingAdas.filter { it.status == PENDING_APPROVAL },
      )
    }
  }

  private fun adjustmentMatchesAdjudication(adjudication: AdasByDateCharged, adjustment: Adjustment): Boolean {
    return adjudication.total == adjustment.effectiveDays && adjudication.dateChargeProved == adjustment.fromDate && adjustment.additionalDaysAwarded != null && adjudication.charges.map { it.chargeNumber }
      .toSet() == adjustment.additionalDaysAwarded!!.adjudicationCharges.map { it.adjudicationId }.toSet()
  }

  private fun lookupAdas(nomsId: String, startOfSentenceEnvelope: LocalDate): List<Ada> {
    val adjudications = prisonApiClient.getAdjudications(nomsId)
    val individualAdjudications =
      adjudications.results.map { prisonApiClient.getAdjudication(nomsId, it.adjudicationNumber) }
    return getAdas(individualAdjudications, startOfSentenceEnvelope)
  }

  private fun getAdasByDateCharged(adas: List<Ada>, filterStatus: ChargeStatus): List<AdasByDateCharged> {
    val adasByDateCharged = adas.filter { it.status == filterStatus }.groupBy { it.dateChargeProved }
    return associateConsecutiveAdas(adasByDateCharged, adas).map {
      it.copy(
        total = calculateTotal(it),
        status = if (filterStatus != AWARDED_OR_PENDING) AdaStatus.valueOf(filterStatus.name) else null,
      )
    }.sortedBy { it.dateChargeProved }
  }

  private fun calculateTotal(adaByDateCharge: AdasByDateCharged): Int {
    return if (adaByDateCharge.charges.size == 1) {
      adaByDateCharge.charges[0].days
    } else {
      val baseCharges = adaByDateCharge.charges.filter { it.consecutiveToSequence == null }
      val consecCharges = adaByDateCharge.charges.filter { it.consecutiveToSequence != null }
      val chains = mutableListOf<MutableList<Ada>>()
      baseCharges.forEach { ada ->
        val chain = mutableListOf(ada)
        chains.add(chain)
        createChain(ada, chain, consecCharges)
      }
      chains.filter { it.isNotEmpty() }.maxOfOrNull { chain -> chain.sumOf { it.days } } ?: 0
    }
  }

  private fun createChain(ada: Ada, chain: MutableList<Ada>, consecCharges: List<Ada>) {
    val consecFrom = consecCharges.find { it.consecutiveToSequence == ada.sequence }
    consecFrom?.let {
      chain.add(it)
      createChain(it, chain, consecCharges)
    }
  }

  private fun associateConsecutiveAdas(
    adasByDateCharged: Map<LocalDate, List<Ada>>,
    adas: List<Ada>,
  ): List<AdasByDateCharged> {
    val consecutiveSourceAdas = getSourceAdaForConsecutive(adas)
    return adasByDateCharged.map { (date, charges) ->
      if (charges.size == 1) {
        AdasByDateCharged(date, mutableListOf(charges[0].copy(toBeServed = "Forthwith")))
      } else {
        val consecutiveAndConcurrentCharges = charges.map { charge ->
          when {
            validConsecutiveSequence(charge, consecutiveSourceAdas) -> {
              val consecutiveAda =
                consecutiveSourceAdas.first { c -> adaHasSequence(charge.consecutiveToSequence!!, c) }
              charge.copy(toBeServed = "Consecutive to ${consecutiveAda.chargeNumber}")
            }

            !validConsecutiveSequence(charge, consecutiveSourceAdas) && !isSourceForConsecutiveChain(
              consecutiveSourceAdas,
              charge,
            ) -> charge.copy(toBeServed = "Concurrent")

            else -> charge.copy(toBeServed = "Forthwith")
          }
        }
        AdasByDateCharged(date, consecutiveAndConcurrentCharges.toMutableList())
      }
    }
  }

  private fun isSourceForConsecutiveChain(consecutiveSourceAdas: List<Ada>, charge: Ada) =
    consecutiveSourceAdas.any { consecutiveAda -> adaHasSequence(charge.sequence, consecutiveAda) }

  private fun validConsecutiveSequence(charge: Ada, consecutiveSourceAdas: List<Ada>): Boolean =
    charge.consecutiveToSequence != null && consecutiveSourceAdas.any { consecutiveAda ->
      adaHasSequence(
        charge.consecutiveToSequence,
        consecutiveAda,
      )
    }

  private fun getSourceAdaForConsecutive(allAdas: List<Ada>): List<Ada> =
    allAdas.filter { ada -> ada.consecutiveToSequence != null && allAdas.any { it.sequence == ada.consecutiveToSequence } }
      .map { consecutiveAda -> allAdas.first { it.sequence == consecutiveAda.consecutiveToSequence } }

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
      ad.hearings != null && ad.hearings.any { h -> prospectiveOrSanctioned(h, startOfSentenceEnvelope) }
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

  private fun sanctionIsAda(s: Sanction) = s.sanctionType == "Additional Days Added"
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
