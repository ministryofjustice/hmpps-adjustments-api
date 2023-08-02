package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FIRST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FROM_DATE_AFTER_TO_DATE
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
  private val EXISTING_ADA = AdjustmentDto(
    id = UUID.randomUUID(),
    bookingId = BOOKING_ID,
    sentenceSequence = null,
    person = PERSON,
    adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
    fromDate = LocalDate.now().minusDays(5),
    toDate = null,
    days = 50,
    additionalDaysAwarded = null,
    unlawfullyAtLarge = null,
  )

  private val EXISTING_RADA = EXISTING_ADA.copy(
    id = UUID.randomUUID(),
    adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
    days = 20,
  )

  @BeforeEach
  fun init() {
    whenever(prisonService.getStartOfSentenceEnvelope(BOOKING_ID)).thenReturn(START_OF_SENTENCE_ENVELOPE)
    whenever(adjustmentService.findByPerson(PERSON)).thenReturn(listOf(EXISTING_ADA, EXISTING_RADA))
  }

  @Nested
  inner class RadaTests {

    val VALID_NEW_RADA = EXISTING_RADA.copy(id = null, days = 4)

    @Test
    fun `RADA days valid`() {
      val result = validationService.validate(VALID_NEW_RADA)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA days missing`() {
      val result = validationService.validate(VALID_NEW_RADA.copy(days = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA days zero`() {
      val result = validationService.validate(VALID_NEW_RADA.copy(days = 0))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA reduce ADAs by more than 50 percent`() {
      val result = validationService.validate(
        VALID_NEW_RADA.copy(
          days = 10,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF)))
      assertThat(result[0].message).isEqualTo("Are you sure you want to add more than 50% of the ADA time for this RADA?")
    }

    @Test
    fun `RADA reduce ADAs by more ADAs`() {
      val result = validationService.validate(
        VALID_NEW_RADA.copy(
          days = 40,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS)))
    }

    @Test
    fun `RADA missing from date`() {
      val result = validationService.validate(VALID_NEW_RADA.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `Future dated radas`() {
      val result = validationService.validate(
        VALID_NEW_RADA.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE)))
    }

    @Test
    fun `Rada before sentence envelope start`() {
      val result = validationService.validate(
        VALID_NEW_RADA.copy(
          fromDate = START_OF_SENTENCE_ENVELOPE.minusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(
            ValidationCode.RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE,
            listOf("1 Jan 2022"),
          ),
        ),
      )
    }

    @Test
    fun `RADA update existing RADA so that days are less than 50 percent`() {
      val result = validationService.validate(EXISTING_RADA.copy(days = 24))
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA update existing RADA so that days are more than 50 percent`() {
      val result = validationService.validate(EXISTING_RADA.copy(days = 26))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF)))
    }

    @Test
    fun `RADA update existing RADA so that days are more than ADAs`() {
      val result = validationService.validate(EXISTING_RADA.copy(days = 51))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS)))
    }
  }

  @Nested
  inner class UalTests {

    val VALID_NEW_UAL = EXISTING_RADA.copy(
      id = null,
      days = null,
      fromDate = LocalDate.now().minusDays(10),
      toDate = LocalDate.now().minusDays(2),
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      unlawfullyAtLarge = UnlawfullyAtLargeDto(UnlawfullyAtLargeType.ESCAPE),
    )

    @Test
    fun `UAL valid`() {
      val result = validationService.validate(VALID_NEW_UAL)
      assertThat(result).isEmpty()
    }

    @Test
    fun `UAL valid same from to date`() {
      val result = validationService.validate(VALID_NEW_UAL.copy(toDate = VALID_NEW_UAL.fromDate))
      assertThat(result).isEmpty()
    }

    @Test
    fun `UAL missing from date`() {
      val result = validationService.validate(VALID_NEW_UAL.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL missing to date`() {
      val result = validationService.validate(VALID_NEW_UAL.copy(toDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_TO_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL to date before from date`() {
      val result = validationService.validate(VALID_NEW_UAL.copy(toDate = VALID_NEW_UAL.fromDate!!.minusDays(1)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_FROM_DATE_AFTER_TO_DATE)))
    }

    @Test
    fun `UAL missing ual type`() {
      val result = validationService.validate(VALID_NEW_UAL.copy(unlawfullyAtLarge = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_TYPE_NOT_NULL)))
    }

    @Test
    fun `UAL before sentence envelope start`() {
      val result = validationService.validate(
        VALID_NEW_UAL.copy(
          fromDate = START_OF_SENTENCE_ENVELOPE.minusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(
            ValidationCode.UAL_DATE_MUST_BE_AFTER_SENTENCE_DATE,
            listOf("1 Jan 2022"),
          ),
        ),
      )
    }

    @Test
    fun `Future dated UAL start date`() {
      val result = validationService.validate(
        VALID_NEW_UAL.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(UAL_FIRST_DATE_CANNOT_BE_FUTURE),
          ValidationMessage(UAL_FROM_DATE_AFTER_TO_DATE),
        ),
      )
    }

    @Test
    fun `Future dated UAL end date`() {
      val result = validationService.validate(
        VALID_NEW_UAL.copy(
          toDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_LAST_DATE_CANNOT_BE_FUTURE)))
    }
  }
}
