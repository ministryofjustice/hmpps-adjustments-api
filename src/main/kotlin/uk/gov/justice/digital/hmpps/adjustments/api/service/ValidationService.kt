package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDetailsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ValidationService(
  private val prisonService: PrisonService,
  private val adjustmentService: AdjustmentsService,
) {

  fun validate(adjustment: AdjustmentDetailsDto): List<ValidationMessage> {
    val startOfSentenceEnvelope = prisonService.getStartOfSentenceEnvelope(adjustment.bookingId)
    if (adjustment.adjustmentType == AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED) {
      return validateRada(adjustment, startOfSentenceEnvelope)
    }
    return emptyList()
  }

  private fun validateRada(adjustment: AdjustmentDetailsDto, startOfSentenceEnvelope: LocalDate): List<ValidationMessage> {
    val validationMessages = mutableListOf<ValidationMessage>()

    val adjustments = adjustmentService.findByPerson(adjustment.person)
    val adaDays = adjustments.filter { it.adjustment.adjustmentType === AdjustmentType.ADDITIONAL_DAYS_AWARDED }.filter { it.adjustment.fromDate!!.isAfter(startOfSentenceEnvelope) }.map { it.adjustment.days!! }.reduceOrNull { acc, it -> acc + it } ?: 0
    val radaDays =
      (
        adjustments.filter { it.adjustment.adjustmentType === AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED }
          .filter { it.adjustment.fromDate!!.isAfter(startOfSentenceEnvelope) }.map { it.adjustment.days!! }
          .reduceOrNull { acc, it -> acc + it } ?: 0
        ) + adjustment.days!!

    if (adaDays < radaDays) {
      validationMessages.add(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS))
    } else if (adaDays / 2 < radaDays) {
      validationMessages.add(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF))
    }

    if (adjustment.fromDate!!.isAfter(LocalDate.now())) {
      validationMessages.add(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE))
    }

    if (adjustment.fromDate.isBefore(startOfSentenceEnvelope)) {
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      validationMessages.add(ValidationMessage(ValidationCode.RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE, listOf(startOfSentenceEnvelope.format(formatter))))
    }

    return validationMessages
  }
}
