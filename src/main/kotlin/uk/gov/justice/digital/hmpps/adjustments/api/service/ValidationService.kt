package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ValidationService(
  private val prisonService: PrisonService,
  private val adjustmentService: AdjustmentsService,
) {

  fun validate(adjustment: AdjustmentDto): List<ValidationMessage> {
    val startOfSentenceEnvelope = prisonService.getStartOfSentenceEnvelope(adjustment.bookingId)
    if (adjustment.adjustmentType == AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED) {
      return validateRada(adjustment, startOfSentenceEnvelope)
    }
    if (adjustment.adjustmentType == AdjustmentType.UNLAWFULLY_AT_LARGE) {
      return validateUal(adjustment, startOfSentenceEnvelope)
    }
    if (adjustment.adjustmentType == AdjustmentType.LAWFULLY_AT_LARGE) {
      return validateLal(adjustment, startOfSentenceEnvelope)
    }
    if (adjustment.adjustmentType == AdjustmentType.SPECIAL_REMISSION) {
      return validateSpecialRemission(adjustment)
    }
    return emptyList()
  }

  private fun validateUal(adjustment: AdjustmentDto, startOfSentenceEnvelope: LocalDate): List<ValidationMessage> {
    val validationMessages = mutableListOf<ValidationMessage>()

    if (adjustment.fromDate == null) {
      validationMessages.add(ValidationMessage(ValidationCode.UAL_FROM_DATE_NOT_NULL))
    } else {
      if (adjustment.fromDate.isAfter(LocalDate.now())) {
        validationMessages.add(ValidationMessage(ValidationCode.UAL_FIRST_DATE_CANNOT_BE_FUTURE))
      }
      if (adjustment.fromDate.isBefore(startOfSentenceEnvelope)) {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        validationMessages.add(ValidationMessage(ValidationCode.UAL_DATE_MUST_BE_AFTER_SENTENCE_DATE, listOf(startOfSentenceEnvelope.format(formatter))))
      }
    }

    if (adjustment.toDate == null) {
      validationMessages.add(ValidationMessage(ValidationCode.UAL_TO_DATE_NOT_NULL))
    } else {
      if (adjustment.toDate.isAfter(LocalDate.now())) {
        validationMessages.add(ValidationMessage(ValidationCode.UAL_LAST_DATE_CANNOT_BE_FUTURE))
      }
    }

    if (adjustment.fromDate != null && adjustment.toDate != null && adjustment.toDate.isBefore(adjustment.fromDate)) {
      validationMessages.add(ValidationMessage(ValidationCode.UAL_FROM_DATE_AFTER_TO_DATE))
    }

    if (adjustment.unlawfullyAtLarge?.type == null) {
      validationMessages.add(ValidationMessage(ValidationCode.UAL_TYPE_NOT_NULL))
    }
    return validationMessages
  }

  private fun validateLal(adjustment: AdjustmentDto, startOfSentenceEnvelope: LocalDate): List<ValidationMessage> {
    val validationMessages = mutableListOf<ValidationMessage>()

    if (adjustment.fromDate == null) {
      validationMessages.add(ValidationMessage(ValidationCode.LAL_FROM_DATE_NOT_NULL))
    } else {
      if (adjustment.fromDate.isAfter(LocalDate.now())) {
        validationMessages.add(ValidationMessage(ValidationCode.LAL_FIRST_DATE_CANNOT_BE_FUTURE))
      }
      if (adjustment.fromDate.isBefore(startOfSentenceEnvelope)) {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        validationMessages.add(ValidationMessage(ValidationCode.LAL_DATE_MUST_BE_AFTER_SENTENCE_DATE, listOf(startOfSentenceEnvelope.format(formatter))))
      }
    }

    if (adjustment.toDate == null) {
      validationMessages.add(ValidationMessage(ValidationCode.LAL_TO_DATE_NOT_NULL))
    } else {
      if (adjustment.toDate.isAfter(LocalDate.now())) {
        validationMessages.add(ValidationMessage(ValidationCode.LAL_LAST_DATE_CANNOT_BE_FUTURE))
      }
    }

    if (adjustment.fromDate != null && adjustment.toDate != null && adjustment.toDate.isBefore(adjustment.fromDate)) {
      validationMessages.add(ValidationMessage(ValidationCode.LAL_FROM_DATE_AFTER_TO_DATE))
    }

    if (adjustment.lawfullyAtLarge?.affectsDates == null) {
      validationMessages.add(ValidationMessage(ValidationCode.LAL_AFFECTS_DATES_NOT_NULL))
    }
    return validationMessages
  }

  private fun validateSpecialRemission(adjustment: AdjustmentDto): List<ValidationMessage> {
    val validationMessages = mutableListOf<ValidationMessage>()

    if (adjustment.specialRemission?.type == null) {
      validationMessages.add(ValidationMessage(ValidationCode.SREM_TYPE_NOT_NULL))
    }
    return validationMessages
  }

  private fun validateRada(adjustment: AdjustmentDto, startOfSentenceEnvelope: LocalDate): List<ValidationMessage> {
    val validationMessages = mutableListOf<ValidationMessage>()

    if (adjustment.days != null && adjustment.days > 0) {
      val adjustments = adjustmentService.findCurrentAdjustments(adjustment.person, AdjustmentStatus.ACTIVE, startOfSentenceEnvelope)
      val adaDays = adjustments.filter { it.adjustmentType === AdjustmentType.ADDITIONAL_DAYS_AWARDED }
        .map { it.days!! }
        .reduceOrNull { acc, it -> acc + it } ?: 0
      val radaDays =
        (
          adjustments.filter { it.adjustmentType === AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED }
            .filter { adjustment.id == null || adjustment.id != it.id }
            .map { it.days!! }
            .reduceOrNull { acc, it -> acc + it } ?: 0
          ) + adjustment.days

      if (adaDays < radaDays) {
        validationMessages.add(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS))
      } else if (adaDays / 2 < radaDays) {
        validationMessages.add(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF))
      }
    } else {
      validationMessages.add(ValidationMessage(ValidationCode.RADA_DAYS_MUST_BE_POSTIVE))
    }

    if (adjustment.fromDate == null) {
      validationMessages.add(ValidationMessage(ValidationCode.RADA_FROM_DATE_NOT_NULL))
    } else {
      if (adjustment.fromDate.isAfter(LocalDate.now())) {
        validationMessages.add(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE))
      }
      if (adjustment.fromDate.isBefore(startOfSentenceEnvelope)) {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        validationMessages.add(ValidationMessage(ValidationCode.RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE, listOf(startOfSentenceEnvelope.format(formatter))))
      }
    }

    return validationMessages
  }
}
