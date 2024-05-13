package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.AdjudicationApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.SystemPrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus
import uk.gov.justice.digital.hmpps.adjustments.api.error.AdjudicationError
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.Ada
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Adjudication
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Punishment
import java.time.LocalDate

@Service
class AdjudicationsLookupService(
  private val adjudicationApiClient: AdjudicationApiClient,
  private val prisonApiClient: PrisonApiClient
) : LookupService {

  override fun lookupAdas(nomsId: String, startOfSentenceEnvelope: LocalDate): List<Ada> {
    val adjudications = adjudicationApiClient.getAdjudications(nomsId)
    val agencies = mutableMapOf<String, String>()

    return adjudications.content.map {
      val punishment =
        it.punishments.last { pun -> pun.type == "PROSPECTIVE_DAYS" || pun.type == "ADDITIONAL_DAYS" }
      val outcome = it.outcomes.last { out -> out.hearing != null }
      Ada(
        dateChargeProved = outcome.hearing!!.dateTimeOfHearing.toLocalDate(),
        days = punishment.schedule.days,
        chargeNumber = it.chargeNumber,
        consecutiveToSequence = punishment.consecutiveChargeNumber,
        heardAt = outcome.hearing.agencyId,
        sequence = it.chargeNumber,
        status = deriveChargeStatus(it, punishment),
      )
    }.filter {
      if (it.status == ChargeStatus.PROSPECTIVE) {
        true
      } else {
        it.dateChargeProved.isAfter(startOfSentenceEnvelope)
      }
    }.map {
      it.copy(
        heardAt = fromAgencyId(it.heardAt, agencies)
      )
    }
  }

  private fun fromAgencyId(heardAt: String?, agencies: MutableMap<String, String>): String? {
    if (heardAt != null) {
      return if (agencies.containsKey(heardAt)) {
        agencies[heardAt]
      } else {
        val prisonDescription = prisonApiClient.getPrison(heardAt).description
        agencies[heardAt] = prisonDescription!!
        prisonDescription
      }
    }
    return null
  }

  private fun deriveChargeStatus(adjudication: Adjudication, punishment: Punishment): ChargeStatus {
    return if (adjudication.status == "QUASHED") {
      ChargeStatus.QUASHED
    } else if (punishment.schedule.suspendedUntil != null) {
      ChargeStatus.SUSPENDED
    } else if (punishment.type == "PROSPECTIVE_DAYS") {
      ChargeStatus.PROSPECTIVE
    } else if (punishment.type == "ADDITIONAL_DAYS") {
      ChargeStatus.AWARDED_OR_PENDING
    } else {
      throw AdjudicationError("unknown adjudication status")
    }
  }
}
