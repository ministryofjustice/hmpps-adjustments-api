package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import kotlin.math.max

@Service
class UnusedDeductionsService(
  val adjustmentService: AdjustmentsService,
  val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,
) {

  fun recalculateUnusedDeductions(offenderNo: String) {
    val adjustments = adjustmentService.findCurrentAdjustments(offenderNo, AdjustmentStatus.ACTIVE, null)
    val anyDpsAdjustments = adjustments.any { it.source == AdjustmentSource.DPS }
    if (anyDpsAdjustments) {
      log.info("Recalculating unused deductions from $offenderNo")
      val deductions = adjustments
        .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

      if (deductions.isEmpty()) {
        setUnusedDeductions(0, adjustments, deductions)
        return
      }

      val allDeductionsEnteredInDps = deductions.all { it.source == AdjustmentSource.DPS }

      if (allDeductionsEnteredInDps) {
        val calculatedUnusedDeductions =
          calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, offenderNo).unusedDeductions

        if (calculatedUnusedDeductions == null) {
          // Couldn't calculate.
          return
        }

        setUnusedDeductions(calculatedUnusedDeductions, adjustments, deductions)
        setEffectiveDays(calculatedUnusedDeductions, deductions)
      }
    }
  }

  private fun setEffectiveDays(unusedDeductions: Int, deductions: List<AdjustmentDto>) {
    var remainingDeductions = unusedDeductions
    // Remand becomes unused first..
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

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
