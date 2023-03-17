package uk.gov.justice.digital.hmpps.adjustments.api.repository

import com.google.common.collect.Lists
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@Transactional
class AdjustmentRepositoryTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun `Create and read adjustment`() {
    val adjustment = Adjustment(
      person = "123ABC",
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().plusDays(5),
      adjustmentType = AdjustmentType.REMAND,
      days = 100,
      daysCalculated = 10,
      legacyData = JacksonUtil.toJsonNode("{\"something\":false}"),
      source = AdjustmentSource.DPS,
      adjustmentHistory = Lists.newArrayList(
        AdjustmentHistory(
          changeType = ChangeType.CREATE,
          changeAt = LocalDateTime.now(),
          changeByUsername = "Someone",
          change = JacksonUtil.toJsonNode("{\"something\":false}")
        )
      )
    )

    val created = adjustmentRepository.save(adjustment)

    val found = adjustmentRepository.findAll()

    assertThat(created).isIn(found)
    assertThat(found[0].adjustmentHistory).isNotEmpty
  }

  @Test
  fun `Not null from date constraint for DPS sources`() {
    val adjustment = Adjustment(
      person = "123ABC",
      fromDate = null,
      toDate = LocalDate.now().plusDays(5),
      source = AdjustmentSource.DPS
    )

    try {
      adjustmentRepository.save(adjustment)
    } catch (e: DataIntegrityViolationException) {
      assertThat(e.localizedMessage).contains("ADJUSTMENT_FROM_DATE_CHECK")
    }
  }
}
