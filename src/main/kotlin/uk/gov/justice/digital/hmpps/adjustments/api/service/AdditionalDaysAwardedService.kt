package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.ADDITIONAL_DAYS_AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Sanction
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.AWARDED_OR_PENDING
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.PROSPECTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.QUASHED
import uk.gov.justice.digital.hmpps.adjustments.api.service.ChargeStatus.SUSPENDED
import java.time.LocalDate
import java.time.LocalDateTime

enum class ChargeStatus { AWARDED_OR_PENDING, SUSPENDED, QUASHED, PROSPECTIVE }

data class ChargeDetails(
  val chargeNumber: Int,
  val toBeServed: String,
  val heardAt: String,
  val status: ChargeStatus,
  val days: Int,
  val sequence: Int,
  val consecutiveToSequence: Int,
)

enum class AdaStatus(alternativeName: String? = null) { AWARDED, PENDING_APPROVAL("PENDING APPROVAL"), SUSPENDED, QUASHED, PROSPECTIVE }

data class AdasByDateCharged(
  val dateChargeProved: LocalDate,
  val charges: MutableList<Ada>,
  val total: Long? = null,
  val status: AdaStatus? = null,
  val adjustmentId: String? = null,
)

data class Ada(
  val dateChargeProved: LocalDate,
  val chargeNumber: Long,
  val toBeServed: String? = null,
  val heardAt: String? = null,
  val status: ChargeStatus,
  val days: Long,
  val sequence: Long? = null,
  val consecutiveToSequence: Long? = null,
)

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
fun sanctionIsProspective(s: Sanction) = s.status == "Prospective" || s.status == "Suspended and Prospective"

fun sanctionIsAda(s: Sanction) = s.sanctionType === "Additional Days Added"
fun isSanctionedAda(sanction: Sanction, hearingDate: LocalDate, startOfSentenceEnvelope: LocalDate) =
  sanctionIsAda(sanction) &&
    !sanctionIsProspective(sanction) &&
    sanction.sanctionDays > 0 && hearingDate >= startOfSentenceEnvelope

fun isProspectiveAda(s: Sanction) = sanctionIsAda(s) && sanctionIsProspective(s)

fun adaHasSequence(sequence: Long, ada: Ada) = sequence == ada.sequence

fun isSuspended(sanction: Sanction): Boolean = sanction.status == "Suspended" ||
  sanction.status == "Suspended and Prospective" ||
  sanction.status == "Period of Suspension Extended" ||
  sanction.status == "Period of Suspension Shortened"

@Service
@Transactional(readOnly = true)
class AdditionalDaysAwardedService(
  private val prisonService: PrisonService,
  private val adjustmentRepository: AdjustmentRepository,
  private val prisonApiClient: PrisonApiClient,
) {
  fun shouldIntercept(nomsId: String): Boolean {
    val adaAdjustments = adjustmentRepository.findByPersonAndAdjustmentType(nomsId, ADDITIONAL_DAYS_AWARDED)
    val anyUnlinkedAdas = adaAdjustments.any { it.additionalDaysAwarded?.adjudicationCharges?.isNotEmpty() ?: false }
    if (anyUnlinkedAdas) return true

    val adas = lookupAdas(nomsId)
    val awardedOrPending: List<AdasByDateCharged> = this.getAdasByDateCharged(adas, AWARDED_OR_PENDING)
    val prospective: List<AdasByDateCharged> = this.getAdasByDateCharged(adas, PROSPECTIVE)

    return false
  }

  private fun lookupAdas(nomsId: String): List<Ada> {
    val startOfSentenceEnvelope = prisonService.getStartOfSentenceEnvelopeExcludingRecalls(nomsId) ?: return true
    val adaAdjustments = adjustmentRepository.findByPersonAndAdjustmentType(nomsId, ADDITIONAL_DAYS_AWARDED)
    val adjudications = prisonApiClient.getAdjudications(nomsId)
    val individualAdjudications =
      adjudications.results.map { prisonApiClient.getAdjudication(nomsId, it.adjudicationNumber) }
    return getAdas(individualAdjudications, startOfSentenceEnvelope)
  }

  private fun getAdasByDateCharged(adas: List<Ada>, filterStatus: ChargeStatus): List<AdasByDateCharged> {

    // TODO check dates ordered correct;y tests
    val adasByDateCharged = adas.filter { it.status == filterStatus }.groupBy { it.dateChargeProved }.toSortedMap()
  }

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
          .forEach { sanction ->  acc.add(Ada(
            dateChargeProved = h.hearingTime.toLocalDate(),
            days = sanction.sanctionDays,
            chargeNumber = cur.adjudicationNumber,
            consecutiveToSequence = sanction.consecutiveSanctionSeq,
            heardAt = h.establishment,
            sequence = sanction.sanctionSeq,
            status = deriveChargeStatus(sanction),
          ))}

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
//
//    private fun getAdasX(
//      individualAdjudications: List<AdjudicationDetail>,
//      startOfSentenceEnvelope: Date,
//    ): Ada[]
//    {
//      const adasToTransform = individualAdjudications . filter (ad =>
//      ad.hearings.some(h => {
//        const hearingDate = new Date(h.hearingTime.substring(0, 10))
//        return h.results.some(r =>
//        r.sanctions.some(s => isProspectiveAda (s) || isSanctionedAda(s, hearingDate, startOfSentenceEnvelope)),
//        )
//      }),
//      )
//
//      adasToTransform.map(a => a . hearings) // chech reliability of consecutive to sequence.. for a given set of adjudocations, where does the consec seq sit?
//      return adasToTransform.reduce((acc: Ada[], cur) => {
//      cur.hearings
//        .filter(h => {
//          const hearingDate = new Date(h.hearingTime.substring(0, 10))
//          return h.results.some(r =>
//          r.sanctions.some(s => isProspectiveAda (s) || isSanctionedAda(s, hearingDate, startOfSentenceEnvelope)),
//          )
//        })
//      .forEach(hearing => {
//      const hearingDate = new Date(hearing.hearingTime.substring(0, 10))
//      const result = hearing . results . find (r =>
//      r.sanctions.some(s => isProspectiveAda (s) || isSanctionedAda(s, hearingDate, startOfSentenceEnvelope)),
//      )
//      result.sanctions
//        .filter(s => isProspectiveAda (s) || isSanctionedAda(s, hearingDate, startOfSentenceEnvelope))
//      .forEach(sanction => {
//      const ada = {
//        dateChargeProved: new Date(hearing.hearingTime.substring(0, 10)),
//        chargeNumber: cur.adjudicationNumber,
//        heardAt: hearing.establishment,
//        status: deriveChargeStatus(cur.adjudicationNumber, sanction),
//        days: sanction.sanctionDays,
//        sequence: sanction.sanctionSeq,
//        consecutiveToSequence: sanction.consecutiveSanctionSeq,
//      } as Ada
//      acc.push(ada)
//    })
//    })
//      return acc
//    }, [])
//    }
}
