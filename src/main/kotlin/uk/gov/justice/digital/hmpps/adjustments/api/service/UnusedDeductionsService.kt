package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.adjustments.api.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnusedDeductionsCalculationResult
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ManualUnusedDeductionsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationResultDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationStatus
import uk.gov.justice.digital.hmpps.adjustments.api.respository.UnusedDeductionsCalculationResultRepository
import java.time.LocalDateTime
import kotlin.math.max

@Service
class UnusedDeductionsService(
  val adjustmentService: AdjustmentsService,
  val prisonService: PrisonService,
  val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,
  val unusedDeductionsCalculationResultRepository: UnusedDeductionsCalculationResultRepository,
) {

  @Transactional
  fun recalculateUnusedDeductions(offenderNo: String) {
    val sentences = prisonService.getSentencesAndStartDateDetails(offenderNo)
    val adjustments = adjustmentService.findCurrentAdjustments(offenderNo, AdjustmentStatus.ACTIVE, sentences.earliestSentenceDate)
    val anyDpsAdjustments = adjustments.any { it.source == AdjustmentSource.DPS }
    if (anyDpsAdjustments) {
      log.info("Recalculating unused deductions from $offenderNo")
      val deductions = adjustments
        .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

      if (deductions.isEmpty()) {
        setUnusedDeductions(0, adjustments, deductions)
        return
      }

      val anyNomisDeductions = deductions.any { it.source == AdjustmentSource.NOMIS }

      if (anyNomisDeductions) {
        setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
      } else {
        val unusedDeductionsResponse =
          calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, offenderNo)
        if (unusedDeductionsResponse.validationMessages.any { it.type != "VALIDATION" }) {
          setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.UNSUPPORTED)
        } else if (sentences.hasRecall) {
          setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.RECALL)
        } else if (unusedDeductionsResponse.validationMessages.isNotEmpty()) {
          setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.VALIDATION)
        } else if (unusedDeductionsResponse.unusedDeductions == null) {
          setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.UNKNOWN)
        } else {
          val unusedDeductions = unusedDeductionsResponse.unusedDeductions
          setUnusedDeductions(unusedDeductions, adjustments, deductions)
          setEffectiveDays(unusedDeductions, deductions)
          setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.CALCULATED)
        }
      }
    } else {
      val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(offenderNo)
      if (result != null) {
        setUnusedDeductionsResult(offenderNo, UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
      }
    }
  }

  private fun setEffectiveDays(unusedDeductions: Int, deductions: List<AdjustmentDto>) {
    var remainingDeductions = unusedDeductions
    // Remand becomes unused first.
    deductions.sortedWith(compareBy({ it.adjustmentType.name }, { it.createdDate!! })).forEach { adjustment ->
      val effectiveDays = max(adjustment.days!! - remainingDeductions, 0)
      remainingDeductions -= adjustment.days
      remainingDeductions = max(remainingDeductions, 0)
      if (effectiveDays != adjustment.effectiveDays) {
        adjustmentService.updateEffectiveDays(adjustment.id!!, AdjustmentEffectiveDaysDto(adjustment.id, effectiveDays, adjustment.person))
      }
    }
  }

  private fun setUnusedDeductions(
    unusedDeductions: Int,
    adjustments: List<AdjustmentDto>,
    deductions: List<AdjustmentDto>,
  ) {
    val unusedDeductionsAdjustment =
      adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }
    if (unusedDeductionsAdjustment != null) {
      if (unusedDeductions == 0) {
        adjustmentService.delete(unusedDeductionsAdjustment.id!!)
      } else {
        if (unusedDeductionsAdjustment.days != unusedDeductions) {
          adjustmentService.update(unusedDeductionsAdjustment.id!!, unusedDeductionsAdjustment.copy(days = unusedDeductions))
        }
      }
    } else {
      if (unusedDeductions > 0) {
        val aDeduction = deductions[0]
        adjustmentService.create(
          listOf(
            aDeduction.copy(
              id = null,
              fromDate = null,
              toDate = null,
              days = unusedDeductions,
              adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
            ),
          ),
        )
      }
    }
  }
  fun setUnusedDaysManually(person: String, manualUnusedDeductionsDto: ManualUnusedDeductionsDto) {
    val adjustments = adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, null)
    val deductions = adjustments
      .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

    setUnusedDeductions(manualUnusedDeductionsDto.days, adjustments, deductions)
    setEffectiveDays(manualUnusedDeductionsDto.days, deductions)
  }

  @Transactional(readOnly = true)
  fun getUnusedDeductionsResult(person: String): UnusedDeductionsCalculationResultDto {
    return unusedDeductionsCalculationResultRepository.findFirstByPerson(person)?.let {
      UnusedDeductionsCalculationResultDto(
        it.person,
        it.calculationAt,
        it.status,
      )
    } ?: UnusedDeductionsCalculationResultDto(person, calculationAt = LocalDateTime.now(), status = UnusedDeductionsCalculationStatus.UNKNOWN)
  }

  private fun setUnusedDeductionsResult(person: String, status: UnusedDeductionsCalculationStatus) {
    val result = unusedDeductionsCalculationResultRepository.findFirstByPerson(person)
    if (result == null) {
      unusedDeductionsCalculationResultRepository.save(
        UnusedDeductionsCalculationResult(
          person = person,
          calculationAt = LocalDateTime.now(),
          status = status,
        ),
      )
    } else {
      unusedDeductionsCalculationResultRepository.save(
        result.copy(
          calculationAt = LocalDateTime.now(),
          status = status,
        ),
      )
    }
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
