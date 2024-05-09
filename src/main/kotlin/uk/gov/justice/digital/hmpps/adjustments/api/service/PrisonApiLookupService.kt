package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.Ada
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Sanction
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PrisonApiLookupService(
  private val prisonApiClient: PrisonApiClient,
) : LookupService {

  override fun lookupAdas(nomsId: String, startOfSentenceEnvelope: LocalDate): List<Ada> {
    val adjudications = prisonApiClient.getAdjudications(nomsId)
    val individualAdjudications =
      adjudications.results.map { prisonApiClient.getAdjudication(nomsId, it.adjudicationNumber) }
    return getAdas(individualAdjudications, startOfSentenceEnvelope)
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
                chargeNumber = cur.adjudicationNumber.toString(),
                consecutiveToSequence = sanction.consecutiveSanctionSeq?.toString(),
                heardAt = h.establishment,
                sequence = sanction.sanctionSeq.toString(),
                status = deriveChargeStatus(sanction),
              ),
            )
          }
      }
      acc
    }
  }

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

  private fun isSuspended(sanction: Sanction): Boolean = sanction.status == "Suspended" ||
    sanction.status == "Suspended and Prospective" ||
    sanction.status == "Period of Suspension Extended" ||
    sanction.status == "Period of Suspension Shortened"

  private fun deriveChargeStatus(sanction: Sanction): ChargeStatus {
    if (isSuspended(sanction)) return ChargeStatus.SUSPENDED
    if (sanction.status == "Quashed") return ChargeStatus.QUASHED
    if (isProspectiveAda(sanction)) return ChargeStatus.PROSPECTIVE
    return ChargeStatus.AWARDED_OR_PENDING
  }

  private fun prospectiveOrSanctioned(hearing: Hearing, startOfSentenceEnvelope: LocalDate): Boolean {
    return hearing.results != null && hearing.results.any { r ->
      r.sanctions != null && r.sanctions.any { s ->
        isProspectiveOrSanctioned(s, hearing.hearingTime, startOfSentenceEnvelope)
      }
    }
  }
  private fun isProspectiveOrSanctioned(
    sanction: Sanction,
    hearingTime: LocalDateTime,
    startOfSentenceEnvelope: LocalDate,
  ) = isProspectiveAda(sanction) || isSanctionedAda(sanction, hearingTime.toLocalDate(), startOfSentenceEnvelope)

  private fun isSanctionedAda(sanction: Sanction, hearingDate: LocalDate, startOfSentenceEnvelope: LocalDate) =
    sanctionIsAda(sanction) &&
      !sanctionIsProspective(sanction) &&
      sanction.sanctionDays > 0 && hearingDate >= startOfSentenceEnvelope

  private fun isProspectiveAda(s: Sanction) = sanctionIsAda(s) && sanctionIsProspective(s)

  companion object {
    const val PRISON_API_LOOKUP_SERVICE = "PRISON-API"
  }
}
