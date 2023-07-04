package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDetailsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import java.time.LocalDate
import java.util.UUID

class ValidationServiceTest {

  private val prisonService = mock<PrisonService>()
  private val adjustmentService = mock<AdjustmentsService>()
  private val validationService = ValidationService(prisonService, adjustmentService)

  private val BOOKING_ID = 1L
  private val PERSON = "ABC123"
  private val START_OF_SENTENCE_ENVELOPE = LocalDate.of(2022, 1, 1)
  private val EXISTING_ADA = AdjustmentDetailsDto(
    bookingId = BOOKING_ID,
    sentenceSequence = null,
    person = PERSON,
    adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
    fromDate = LocalDate.now().minusDays(5),
    toDate = null,
    days = 50,
    additionalDaysAwarded = null,
  )

  private val EXISTING_RADA = EXISTING_ADA.copy(
    adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
    days = 20,
  )

  @BeforeEach
  fun init() {
    whenever(prisonService.getStartOfSentenceEnvelope(BOOKING_ID)).thenReturn(START_OF_SENTENCE_ENVELOPE)
    whenever(adjustmentService.findByPerson(PERSON)).thenReturn(listOf(AdjustmentDto(UUID.randomUUID(), EXISTING_ADA), AdjustmentDto(UUID.randomUUID(), EXISTING_RADA)))
  }

  @Nested
  inner class RadaTests {

    val VALID_RADA = EXISTING_RADA.copy(days = 4)

    @Test
    fun `RADA days valid`() {
      val result = validationService.validate(VALID_RADA)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA reduce ADAs by more than 50 percent`() {
      val result = validationService.validate(
        VALID_RADA.copy(
          days = 10,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF)))
      assertThat(result[0].message).isEqualTo("Are you sure, as this reduction is more than 50% of the total additional days awarded?")
    }

    @Test
    fun `RADA reduce ADAs by more ADAs`() {
      val result = validationService.validate(
        VALID_RADA.copy(
          days = 40,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS)))
    }

    @Test
    fun `Future dated radas`() {
      val result = validationService.validate(
        VALID_RADA.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE)))
    }

    @Test
    fun `Rada before sentence envelope start`() {
      val result = validationService.validate(
        VALID_RADA.copy(
          fromDate = START_OF_SENTENCE_ENVELOPE.minusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(
            ValidationCode.RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE,
            listOf("01/01/2022"),
          ),
        ),
      )
    }
  }
}
