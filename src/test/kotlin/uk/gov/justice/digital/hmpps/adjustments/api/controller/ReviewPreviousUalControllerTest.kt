package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeReviewRequest
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsDomainEventService
import uk.gov.justice.digital.hmpps.adjustments.api.service.ReviewPreviousUalService
import java.time.LocalDate
import java.util.UUID

class ReviewPreviousUalControllerTest {

  private val reviewPreviousUalService: ReviewPreviousUalService = mock()
  private val adjustmentsDomainEventService: AdjustmentsDomainEventService = mock()

  private val controller = ReviewPreviousUalController(reviewPreviousUalService, adjustmentsDomainEventService)

  @Test
  fun `should return UAL`() {
    val expected = listOf(
      PreviousUnlawfullyAtLargeAdjustmentForReview(
        id = UUID.randomUUID(),
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2021, 1, 11),
        days = 10,
        type = UnlawfullyAtLargeType.RELEASE_IN_ERROR,
        prisonName = "Brixton (HMP)",
        prisonId = "BXI",
      ),
      PreviousUnlawfullyAtLargeAdjustmentForReview(
        id = UUID.randomUUID(),
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2022, 1, 1),
        days = 364,
        type = UnlawfullyAtLargeType.ESCAPE,
        prisonName = "Brixton (HMP)",
        prisonId = "BXI",
      ),
    )

    whenever(reviewPreviousUalService.findPreviousUalToReview("A1234BC")).thenReturn(expected)

    val result = controller.findPreviousUalToReview("A1234BC")

    assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `should confirm UAL and send events for any created adjustments`() {
    val firstCreatedId = UUID.randomUUID()
    val secondCreatedId = UUID.randomUUID()

    val firstMetadata = AdjustmentEventMetadata(
      eventType = AdjustmentEventType.ADJUSTMENT_CREATED,
      ids = listOf(firstCreatedId),
      person = "A1234BC",
      source = AdjustmentSource.DPS,
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      isLast = false,
    )
    val secondMetadata = AdjustmentEventMetadata(
      eventType = AdjustmentEventType.ADJUSTMENT_CREATED,
      ids = listOf(secondCreatedId),
      person = "A1234BC",
      source = AdjustmentSource.DPS,
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      isLast = true,
    )
    whenever(reviewPreviousUalService.submitPreviousUalReview(eq("A1234BC"), any())).thenReturn(listOf(firstMetadata, secondMetadata))

    controller.confirmPreviousUal(
      "A1234BC",
      PreviousUnlawfullyAtLargeReviewRequest(
        acceptedAdjustmentIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
        rejectedAdjustmentIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
      ),
    )

    val eventCaptor = argumentCaptor<AdjustmentEventMetadata>()
    verify(reviewPreviousUalService).submitPreviousUalReview(eq("A1234BC"), any())
    verify(adjustmentsDomainEventService, times(2)).raiseAdjustmentEvent(eventCaptor.capture())
    assertThat(eventCaptor.firstValue).isEqualTo(firstMetadata)
    assertThat(eventCaptor.secondValue).isEqualTo(secondMetadata)
  }

  @Test
  fun `should confirm UAL with no created adjustments`() {
    whenever(reviewPreviousUalService.submitPreviousUalReview(eq("A1234BC"), any())).thenReturn(emptyList())

    controller.confirmPreviousUal(
      "A1234BC",
      PreviousUnlawfullyAtLargeReviewRequest(
        acceptedAdjustmentIds = listOf(),
        rejectedAdjustmentIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
      ),
    )

    verify(reviewPreviousUalService).submitPreviousUalReview(eq("A1234BC"), any())
    verify(adjustmentsDomainEventService, never()).raiseAdjustmentEvent(any())
  }
}
