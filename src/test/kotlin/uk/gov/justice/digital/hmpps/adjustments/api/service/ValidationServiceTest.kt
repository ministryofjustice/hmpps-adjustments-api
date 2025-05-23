package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.LawfullyAtLargeAffectsDates
import uk.gov.justice.digital.hmpps.adjustments.api.enums.SpecialRemissionType.MERITORIOUS_CONDUCT
import uk.gov.justice.digital.hmpps.adjustments.api.enums.TimeSpentInCustodyAbroadDocumentationSource
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.LawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.SpecialRemissionDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TimeSpentAsAnAppealApplicantDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TimeSpentInCustodyAbroadDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_AFFECTS_DATES_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_DATE_MUST_BE_AFTER_SENTENCE_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_FIRST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_FROM_DATE_AFTER_TO_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_FROM_DATE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_LAST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.LAL_TO_DATE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.MORE_RADAS_THAN_ADAS
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.RADA_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.RADA_DAYS_MUST_BE_POSTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.RADA_FROM_DATE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.RADA_REDUCES_BY_MORE_THAN_HALF
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.SREM_TYPE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.TCA_DOCUMENTATION_SOURCE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.TSA_COURT_OF_APPEAL_REFERENCE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_DATE_MUST_BE_AFTER_SENTENCE_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FIRST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FROM_DATE_AFTER_TO_DATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_FROM_DATE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_LAST_DATE_CANNOT_BE_FUTURE
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_TO_DATE_NOT_NULL
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode.UAL_TYPE_NOT_NULL
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
    days = 50,
    effectiveDays = 50,
    additionalDaysAwarded = null,
    unlawfullyAtLarge = null,
    lawfullyAtLarge = null,
    specialRemission = null,
    remand = null,
    taggedBail = null,
    timeSpentInCustodyAbroad = null,
    timeSpentAsAnAppealApplicant = null,
    lastUpdatedDate = LocalDateTime.now(),
    createdDate = LocalDateTime.now(),
    lastUpdatedBy = "Person",
    status = AdjustmentStatus.ACTIVE,
    recallId = null,
  )

  private val existingRada = existingAda.copy(
    id = UUID.randomUUID(),
    adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
    days = 20,
  )

  private val existingNomisRada = existingRada.copy(
    id = UUID.randomUUID(),
    days = 20,
    effectiveDays = 20,
  )

  @BeforeEach
  fun init() {
    whenever(prisonService.getStartOfSentenceEnvelope(bookingId)).thenReturn(startOfSentenceOverlap)
    whenever(adjustmentService.findCurrentAdjustments(person, listOf(AdjustmentStatus.ACTIVE), true)).thenReturn(listOf(existingAda, existingRada))
  }

  @Nested
  inner class RadaTests {

    val validNewRada = existingRada.copy(id = null, days = 4)

    @Test
    fun `RADA days valid`() {
      val result = validationService.validate(validNewRada)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA days valid if existing rada is from NOMIS`() {
      whenever(adjustmentService.findCurrentAdjustments(person, listOf(AdjustmentStatus.ACTIVE), true)).thenReturn(listOf(existingAda, existingNomisRada))
      val result = validationService.validate(validNewRada)
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA days missing`() {
      val result = validationService.validate(validNewRada.copy(days = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA days zero`() {
      val result = validationService.validate(validNewRada.copy(days = 0))
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_DAYS_MUST_BE_POSTIVE)))
    }

    @Test
    fun `RADA reduce ADAs by more than 50 percent`() {
      val result = validationService.validate(
        validNewRada.copy(
          days = 10,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_REDUCES_BY_MORE_THAN_HALF)))
      assertThat(result[0].message).isEqualTo("Are you sure you want to add more than 50% of the ADA time for this RADA?")
    }

    @Test
    fun `RADA reduce ADAs by more ADAs`() {
      val result = validationService.validate(
        validNewRada.copy(
          days = 40,
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(MORE_RADAS_THAN_ADAS)))
    }

    @Test
    fun `RADA missing from date`() {
      val result = validationService.validate(validNewRada.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `Future dated radas`() {
      val result = validationService.validate(
        validNewRada.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_DATE_CANNOT_BE_FUTURE)))
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
            RADA_DATA_MUST_BE_AFTER_SENTENCE_DATE,
            listOf("1 Jan 2022"),
          ),
        ),
      )
    }

    @Test
    fun `RADA update existing RADA so that days are less than 50 percent`() {
      val result = validationService.validate(existingRada.copy(days = 24))
      assertThat(result).isEmpty()
    }

    @Test
    fun `RADA update existing RADA so that days are more than 50 percent`() {
      val result = validationService.validate(existingRada.copy(days = 26))
      assertThat(result).isEqualTo(listOf(ValidationMessage(RADA_REDUCES_BY_MORE_THAN_HALF)))
    }

    @Test
    fun `RADA update existing RADA so that days are more than ADAs`() {
      val result = validationService.validate(existingRada.copy(days = 51))
      assertThat(result).isEqualTo(listOf(ValidationMessage(MORE_RADAS_THAN_ADAS)))
    }
  }

  @Nested
  inner class UalTests {

    val validNewUal = existingRada.copy(
      id = null,
      days = 9,
      fromDate = LocalDate.now().minusDays(10),
      toDate = LocalDate.now().minusDays(2),
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      unlawfullyAtLarge = UnlawfullyAtLargeDto(UnlawfullyAtLargeType.ESCAPE),
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
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL missing to date`() {
      val result = validationService.validate(validNewUal.copy(toDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_TO_DATE_NOT_NULL)))
    }

    @Test
    fun `UAL to date before from date`() {
      val result = validationService.validate(validNewUal.copy(toDate = validNewUal.fromDate!!.minusDays(1)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_FROM_DATE_AFTER_TO_DATE)))
    }

    @Test
    fun `UAL missing ual type`() {
      val result = validationService.validate(validNewUal.copy(unlawfullyAtLarge = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_TYPE_NOT_NULL)))
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
            UAL_DATE_MUST_BE_AFTER_SENTENCE_DATE,
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
      assertThat(result).isEqualTo(listOf(ValidationMessage(UAL_LAST_DATE_CANNOT_BE_FUTURE)))
    }
  }

  @Nested
  inner class LalTests {

    val validNewLal = existingRada.copy(
      id = null,
      days = 9,
      fromDate = LocalDate.now().minusDays(10),
      toDate = LocalDate.now().minusDays(2),
      adjustmentType = AdjustmentType.LAWFULLY_AT_LARGE,
      lawfullyAtLarge = LawfullyAtLargeDto(LawfullyAtLargeAffectsDates.YES),
    )

    @Test
    fun `LAL valid`() {
      val result = validationService.validate(validNewLal)
      assertThat(result).isEmpty()
    }

    @Test
    fun `LAL valid same from to date`() {
      val result = validationService.validate(validNewLal.copy(toDate = validNewLal.fromDate))
      assertThat(result).isEmpty()
    }

    @Test
    fun `LAL missing from date`() {
      val result = validationService.validate(validNewLal.copy(fromDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(LAL_FROM_DATE_NOT_NULL)))
    }

    @Test
    fun `LAL missing to date`() {
      val result = validationService.validate(validNewLal.copy(toDate = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(LAL_TO_DATE_NOT_NULL)))
    }

    @Test
    fun `LAL to date before from date`() {
      val result = validationService.validate(validNewLal.copy(toDate = validNewLal.fromDate!!.minusDays(1)))
      assertThat(result).isEqualTo(listOf(ValidationMessage(LAL_FROM_DATE_AFTER_TO_DATE)))
    }

    @Test
    fun `LAL missing affects dates`() {
      val result = validationService.validate(validNewLal.copy(lawfullyAtLarge = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(LAL_AFFECTS_DATES_NOT_NULL)))
    }

    @Test
    fun `LAL before sentence envelope start`() {
      val result = validationService.validate(
        validNewLal.copy(
          fromDate = startOfSentenceOverlap.minusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(
            LAL_DATE_MUST_BE_AFTER_SENTENCE_DATE,
            listOf("1 Jan 2022"),
          ),
        ),
      )
    }

    @Test
    fun `Future dated LAL start date`() {
      val result = validationService.validate(
        validNewLal.copy(
          fromDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(
        listOf(
          ValidationMessage(LAL_FIRST_DATE_CANNOT_BE_FUTURE),
          ValidationMessage(LAL_FROM_DATE_AFTER_TO_DATE),
        ),
      )
    }

    @Test
    fun `Future dated LAL end date`() {
      val result = validationService.validate(
        validNewLal.copy(
          toDate = LocalDate.now().plusDays(1),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(LAL_LAST_DATE_CANNOT_BE_FUTURE)))
    }
  }

  @Nested
  inner class SpecialRemissionTests {

    val validNewSpecialRemission = existingRada.copy(
      id = null,
      days = 9,
      adjustmentType = AdjustmentType.SPECIAL_REMISSION,
      specialRemission = SpecialRemissionDto(MERITORIOUS_CONDUCT),
    )

    @Test
    fun `Special Remission valid`() {
      val result = validationService.validate(validNewSpecialRemission)
      assertThat(result).isEmpty()
    }

    @Test
    fun `Special Remission with no type is not valid`() {
      val result = validationService.validate(validNewSpecialRemission.copy(specialRemission = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(SREM_TYPE_NOT_NULL)))
    }
  }

  @Nested
  inner class TimeSpentInCustodyAbroadTests {

    val validTimeSpentInCustodyAbroad = existingRada.copy(
      id = null,
      days = 9,
      adjustmentType = AdjustmentType.CUSTODY_ABROAD,
      timeSpentInCustodyAbroad = TimeSpentInCustodyAbroadDto(TimeSpentInCustodyAbroadDocumentationSource.COURT_WARRANT, listOf(42)),
    )

    @Test
    fun `Time spent in custody abroad is valid`() {
      val result = validationService.validate(validTimeSpentInCustodyAbroad)
      assertThat(result).isEmpty()
    }

    @Test
    fun `Time spent in custody abroad with no documentation source is not valid`() {
      val result = validationService.validate(validTimeSpentInCustodyAbroad.copy(timeSpentInCustodyAbroad = null))
      assertThat(result).isEqualTo(listOf(ValidationMessage(TCA_DOCUMENTATION_SOURCE_NOT_NULL)))
    }
  }

  @Nested
  inner class TimeSpentAsAnAppealApplicant {

    val validTimeSpentAsAnAppealApplicant = existingRada.copy(
      id = null,
      days = 9,
      adjustmentType = AdjustmentType.APPEAL_APPLICANT,
      timeSpentAsAnAppealApplicant = TimeSpentAsAnAppealApplicantDto("AF3459678", listOf(2453)),
    )

    @Test
    fun `Time spent as an appeal applicant with valid court of appeal reference number`() {
      val result = validationService.validate(validTimeSpentAsAnAppealApplicant)
      assertThat(result).isEmpty()
    }

    @Test
    fun `Court of appeal reference number must not be null`() {
      val result = validationService.validate(
        validTimeSpentAsAnAppealApplicant.copy(timeSpentAsAnAppealApplicant = null),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(TSA_COURT_OF_APPEAL_REFERENCE_NOT_NULL)))
    }

    @ParameterizedTest
    @CsvSource(
      "abc123, TSA_COURT_OF_APPEAL_REFERENCE_WRONG_LENGTH",
      "1235467, TSA_COURT_OF_APPEAL_REFERENCE_WRONG_LENGTH",
      "31CharacterStringIsNotValidHere, TSA_COURT_OF_APPEAL_REFERENCE_WRONG_LENGTH",
      "WA132156=, TSA_COURT_OF_APPEAL_REFERENCE_INVALID_CHARACTERS",
      "151BAC\'\"!@£$%^&*()_||\\/, TSA_COURT_OF_APPEAL_REFERENCE_INVALID_CHARACTERS",
      "151BAC=/*-+?~|{}[], TSA_COURT_OF_APPEAL_REFERENCE_INVALID_CHARACTERS",
    )
    fun `test if string is a palindrome`(candidate: String, expected: ValidationCode) {
      val result = validationService.validate(
        validTimeSpentAsAnAppealApplicant.copy(
          timeSpentAsAnAppealApplicant = TimeSpentAsAnAppealApplicantDto(candidate, listOf(2453456)),
        ),
      )
      assertThat(result).isEqualTo(listOf(ValidationMessage(expected)))
    }
  }
}
