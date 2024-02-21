package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.controller.AdjustmentControllerIntTest
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FIRST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FROM_DATE_AFTER_TO_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ValidationServiceTest {

  private val prisonService = mock<PrisonService>()
  private val adjustmentService = mock<AdjustmentsService>()
  private val validationService = ValidationService(prisonService, adjustmentService)

  private val bookingId = 1L
  private val person = "ABC123"
  private val startOfSentenceOverlap = LocalDate.of(2022, 1, 1)
  private val existingAda = AdjustmentDto(
    id = UUID.randomUUID(),
    bookingId = bookingId,
    sentenceSequence = null,
    person = person,
    adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
    toDate = null,
    fromDate = LocalDate.now().minusDays(5),
    daysTotal = 50,
    effectiveDays = 50,
    additionalDaysAwarded = null,
    unlawfullyAtLarge = null,
    remand = null,
    taggedBail = null,
    lastUpdatedDate = LocalDateTime.now(),
    createdDate = LocalDateTime.now(),
    lastUpdatedBy = "Person",
    status = AdjustmentStatus.ACTIVE,
  )

  private val existingRada = existingAda.copy(
    id = UUID.randomUUID(),
    adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
    daysTotal = 20,
  )

  private val existingNomisRada = existingRada.copy(
    id = UUID.randomUUID(),
    daysTotal = 20,
    effectiveDays = 20,
  )

  @BeforeEach
  fun init() {
    whenever(prisonService.getStartOfSentenceEnvelope(bookingId)).thenReturn(startOfSentenceOverlap)
    whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, startOfSentenceOverlap)).thenReturn(listOf(existingAda, existingRada))
  }

  @Nested
  inner class RadaTests {

    val validNewRada = AdjustmentControllerIntTest.adjustmentDTOToEditableAdjustmentDTO(existingRada.copy(id = null, daysTotal = 4))

    @Test
    fun `RADA days valid`() {
      val result = validationService.validate(validNewRada)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA days valid if existing rada is from NOMIS`() {
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, startOfSentenceOverlap)).thenReturn(listOf(existingAda, existingNomisRada))
      val result = validationService.validate(validNewRada)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA days missing`() {
      val result = validationService.validate(validNewRada.copy(days = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA days zero`() {
      val result = validationService.validate(validNewRada.copy(days = 0))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA reduce ADAs by more than 50 percent`() {
      val result = validationService.validate(
        validNewRada.copy(
          days = 10,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF)))
      assertThat(result[0].message).isEqualTo("Are you sure you want to add more than 50% of the ADA time for this RADA?")
    }

    @Test
    fun `RADA reduce ADAs by more ADAs`() {
      val result = validationService.validate(
        validNewRada.copy(
          days = 40,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS)))
    }

    @Test
    fun `RADA missing from date`() {
      val result = validationService.validate(validNewRada.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `Future dated radas`() {
      val result = validationService.validate(
        validNewRada.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE)))
    }

    @Test
    fun `Rada before sentence envelope start`() {
      val result = validationService.validate(
        validNewRada.copy(
          fromDate = startOfSentenceOverlap.minusDays(1),
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
      val result = validationService.validate(AdjustmentControllerIntTest.adjustmentDTOToEditableAdjustmentDTO(existingRada.copy(daysTotal = 24)))
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA update existing RADA so that days are more than 50 percent`() {
      val result = validationService.validate(AdjustmentControllerIntTest.adjustmentDTOToEditableAdjustmentDTO(existingRada.copy(daysTotal = 26)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF)))
    }

    @Test
    fun `RADA update existing RADA so that days are more than ADAs`() {
      val result = validationService.validate(AdjustmentControllerIntTest.adjustmentDTOToEditableAdjustmentDTO(existingRada.copy(daysTotal = 51)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS)))
    }
  }

  @Nested
  inner class UalTests {

    val validNewUal = AdjustmentControllerIntTest.adjustmentDTOToEditableAdjustmentDTO(
      existingRada.copy(
        id = null,
        daysTotal = 9,
        fromDate = LocalDate.now().minusDays(10),
        toDate = LocalDate.now().minusDays(2),
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        unlawfullyAtLarge = UnlawfullyAtLargeDto(UnlawfullyAtLargeType.ESCAPE),
      ),
    )

    @Test
    fun `UAL valid`() {
      val result = validationService.validate(validNewUal)
      assertThat(result).isEmpty()
    }

    @Test
    fun `UAL valid same from to date`() {
      val result = validationService.validate(validNewUal.copy(toDate = validNewUal.fromDate))
      assertThat(result).isEmpty()
    }

    @Test
    fun `UAL missing from date`() {
      val result = validationService.validate(validNewUal.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL missing to date`() {
      val result = validationService.validate(validNewUal.copy(toDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_TO_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL to date before from date`() {
      val result = validationService.validate(validNewUal.copy(toDate = validNewUal.fromDate!!.minusDays(1)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_FROM_DATE_AFTER_TO_DATE)))
    }

    @Test
    fun `UAL missing ual type`() {
      val result = validationService.validate(validNewUal.copy(unlawfullyAtLarge = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_TYPE_NOT_NULL)))
    }

    @Test
    fun `UAL before sentence envelope start`() {
      val result = validationService.validate(
        validNewUal.copy(
          fromDate = startOfSentenceOverlap.minusDays(1),
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
        validNewUal.copy(
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
        validNewUal.copy(
          toDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(ValidationCode.UAL_LAST_DATE_CANNOT_BE_FUTURE)))
    }
  }
}
