package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeReviewRequest
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.util.UUID

@Sql("classpath:test_data/reset-data.sql")
class ReviewPreviousUalIntTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun `do not show previous UAL when there are no adjustments`() {
    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `do not show previous UAL when it is before the sentence date`() {
    adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).minusDays(20),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).minusDays(10),
        effectiveDays = 9,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `do not show previous UAL when it ends on the sentence date`() {
    adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).minusDays(20),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE),
        effectiveDays = 19,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `show previous UAL when it starts before the earliest sentence date but ends after`() {
    val previousUalEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).minusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        effectiveDays = 19,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).containsExactly(
      PreviousUnlawfullyAtLargeAdjustmentForReview(
        id = previousUalEntity.id,
        fromDate = previousUalEntity.fromDate!!,
        toDate = previousUalEntity.toDate!!,
        days = 19,
        type = UnlawfullyAtLargeType.ESCAPE,
        prisonName = null,
        prisonId = null,
      ),
    )
  }

  @Test
  fun `do not show previous UAL when it is already on the current booking`() {
    adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).minusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        effectiveDays = 19,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = true,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `do not show previous UAL when the effective days is 0 as some legacy UAL start and end on the same day`() {
    adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        effectiveDays = 0,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `do not show previous UAL when the effective days is negative as some legacy UAL have invalid days`() {
    adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(9),
        effectiveDays = -1,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).isEmpty()
  }

  @Test
  fun `show previous UAL when it starts and ends after the earliest sentence date`() {
    val previousUalEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        effectiveDays = 9,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    assertThat(previousUalToReview).containsExactly(
      PreviousUnlawfullyAtLargeAdjustmentForReview(
        id = previousUalEntity.id,
        fromDate = previousUalEntity.fromDate!!,
        toDate = previousUalEntity.toDate!!,
        days = 9,
        type = UnlawfullyAtLargeType.RECALL,
        prisonName = null,
        prisonId = null,
      ),
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "ACTIVE,true",
      "INACTIVE,true",
      "DELETED,false",
      "INACTIVE_WHEN_DELETED,false",
    ],
  )
  fun `show active and inactive but not deleted previous UAL`(status: AdjustmentStatus, shouldShow: Boolean) {
    val previousUalEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        effectiveDays = 9,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = status,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    val previousUalToReview = getPreviousUalToReview()

    if (shouldShow) {
      assertThat(previousUalToReview).containsExactly(
        PreviousUnlawfullyAtLargeAdjustmentForReview(
          id = previousUalEntity.id,
          fromDate = previousUalEntity.fromDate!!,
          toDate = previousUalEntity.toDate!!,
          days = 9,
          type = UnlawfullyAtLargeType.RECALL,
          prisonName = null,
          prisonId = null,
        ),
      )
    } else {
      assertThat(previousUalToReview).isEmpty()
    }
  }

  @Test
  fun `can accept and reject previous UAL and then no longer show it`() {
    val adjustmentToKeepEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        effectiveDays = 11,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )
    val adjustmentToRejectEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(30),
        effectiveDays = 11,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.ACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    assertThat(getPreviousUalToReview()).hasSize(2)

    webTestClient
      .put()
      .uri("/adjustments/person/${PrisonApiExtension.PRISONER_ID}/review-previous-ual")
      .headers(setAdjustmentsRWAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        PreviousUnlawfullyAtLargeReviewRequest(
          acceptedAdjustmentIds = listOf(adjustmentToKeepEntity.id),
          rejectedAdjustmentIds = listOf(adjustmentToRejectEntity.id),
        ),
      )
      .exchange()
      .expectStatus().isAccepted

    assertThat(getPreviousUalToReview()).hasSize(0)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 2 }

    val messages = getLatestMessage()!!.messages()
    assertThat(messages.find { it.body().contains(AdjustmentEventType.ADJUSTMENT_CREATED.value) }).describedAs { "created event" }.isNotNull
    assertThat(messages.find { it.body().contains(AdjustmentEventType.ADJUSTMENT_REVIEWED_PREVIOUS_UAL_PERIODS.value) }).describedAs { "reviewed event" }.isNotNull
    val newUal = adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(PrisonApiExtension.PRISONER_ID, AdjustmentType.UNLAWFULLY_AT_LARGE)
    assertThat(newUal).hasSize(1)
    assertThat(newUal[0].fromDate).isEqualTo(adjustmentToKeepEntity.fromDate)
    assertThat(newUal[0].toDate).isEqualTo(adjustmentToKeepEntity.toDate)
    assertThat(newUal[0].effectiveDays).isEqualTo(adjustmentToKeepEntity.effectiveDays)
    assertThat(newUal[0].unlawfullyAtLarge?.type).isEqualTo(UnlawfullyAtLargeType.RECALL)
    assertThat(newUal[0].currentPeriodOfCustody).isTrue
  }

  @Test
  fun `can accept legacy UAL that didn't have a type`() {
    val legacyEntity = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        effectiveDays = 11,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = null,
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    assertThat(getPreviousUalToReview()).hasSize(1)

    webTestClient
      .put()
      .uri("/adjustments/person/${PrisonApiExtension.PRISONER_ID}/review-previous-ual")
      .headers(setAdjustmentsRWAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        PreviousUnlawfullyAtLargeReviewRequest(
          acceptedAdjustmentIds = listOf(legacyEntity.id),
          rejectedAdjustmentIds = emptyList(),
        ),
      )
      .exchange()
      .expectStatus().isAccepted

    assertThat(getPreviousUalToReview()).hasSize(0)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 2 }

    val messages = getLatestMessage()!!.messages()
    assertThat(messages.find { it.body().contains(AdjustmentEventType.ADJUSTMENT_CREATED.value) }).describedAs { "created event" }.isNotNull
    assertThat(messages.find { it.body().contains(AdjustmentEventType.ADJUSTMENT_REVIEWED_PREVIOUS_UAL_PERIODS.value) }).describedAs { "reviewed event" }.isNotNull
    val newUal = adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(PrisonApiExtension.PRISONER_ID, AdjustmentType.UNLAWFULLY_AT_LARGE)
    assertThat(newUal).hasSize(1)
    assertThat(newUal[0].fromDate).isEqualTo(legacyEntity.fromDate)
    assertThat(newUal[0].toDate).isEqualTo(legacyEntity.toDate)
    assertThat(newUal[0].effectiveDays).isEqualTo(legacyEntity.effectiveDays)
    assertThat(newUal[0].unlawfullyAtLarge).isNull()
    assertThat(newUal[0].currentPeriodOfCustody).isTrue
  }

  @Test
  fun `rejecting all adjustments still generates an event to clear things to do cache`() {
    val firstAdjustment = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(10),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        effectiveDays = 9,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.INACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RECALL),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )
    val secondAdjustment = adjustmentRepository.save(
      Adjustment(
        id = UUID.randomUUID(),
        person = PrisonApiExtension.PRISONER_ID,
        fromDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(20),
        toDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE).plusDays(30),
        effectiveDays = 9,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        currentPeriodOfCustody = false,
        status = AdjustmentStatus.ACTIVE,
        source = AdjustmentSource.DPS,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
        adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE)),
      ),
    )

    assertThat(getPreviousUalToReview()).hasSize(2)

    webTestClient
      .put()
      .uri("/adjustments/person/${PrisonApiExtension.PRISONER_ID}/review-previous-ual")
      .headers(setAdjustmentsRWAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        PreviousUnlawfullyAtLargeReviewRequest(
          acceptedAdjustmentIds = listOf(),
          rejectedAdjustmentIds = listOf(firstAdjustment.id, secondAdjustment.id),
        ),
      )
      .exchange()
      .expectStatus().isAccepted

    assertThat(getPreviousUalToReview()).hasSize(0)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }

    val messages = getLatestMessage()!!.messages()
    assertThat(messages.find { it.body().contains(AdjustmentEventType.ADJUSTMENT_REVIEWED_PREVIOUS_UAL_PERIODS.value) }).describedAs { "reviewed event" }.isNotNull
  }

  private fun getPreviousUalToReview(): List<PreviousUnlawfullyAtLargeAdjustmentForReview?>? = webTestClient
    .get()
    .uri("/adjustments/person/${PrisonApiExtension.PRISONER_ID}/review-previous-ual")
    .headers(setAdjustmentsROAuth())
    .exchange()
    .expectStatus().isOk
    .expectBodyList<PreviousUnlawfullyAtLargeAdjustmentForReview>()
    .returnResult()
    .responseBody
}
