package uk.gov.justice.digital.hmpps.adjustments.api.controller

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.config.ErrorResponse
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjudicationCharges
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.DELETED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.TAGGED_BAIL
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType.ESCAPE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType.RECALL
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.listener.REMAND_ID
import uk.gov.justice.digital.hmpps.adjustments.api.listener.TAGGED_BAIL_ID
import uk.gov.justice.digital.hmpps.adjustments.api.listener.UNUSED_DEDUCTIONS_PRISONER_ID
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ManualUnusedDeductionsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.AdjustmentsTransactionalService
import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.CalculateReleaseDatesApiExtension
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class AdjustmentControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  lateinit var entityManager: EntityManager

  @Nested
  inner class GeneralTests {
    @Test
    @Transactional
    fun create() {
      val id = createAnAdjustment()
      val adjustment = adjustmentRepository.findById(id).get()

      assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
      assertThat(adjustment.adjustmentHistory).singleElement()
      assertThat(adjustment.adjustmentHistory[0].changeType).isEqualTo(ChangeType.CREATE)
      assertThat(adjustment.adjustmentHistory[0].changeByUsername).isEqualTo("Test User")
      assertThat(adjustment.adjustmentHistory[0].changeSource).isEqualTo(AdjustmentSource.DPS)
      assertThat(adjustment.source).isEqualTo(AdjustmentSource.DPS)
      assertThat(adjustment.status).isEqualTo(ACTIVE)

      assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
      assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(2))
      assertThat(adjustment.daysCalculated).isEqualTo(4)
      assertThat(adjustment.effectiveDays).isEqualTo(4)

      val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
      assertThat(legacyData).isEqualTo(
        LegacyData(
          bookingId = PrisonApiExtension.BOOKING_ID,
          sentenceSequence = 1,
          postedDate = LocalDate.now(),
          comment = null,
          type = null,
          chargeIds = listOf(9991),
        ),
      )

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
      val latestMessage: String = getLatestMessage()!!.messages()[0].body()
      assertThat(latestMessage).contains(adjustment.id.toString())
      assertThat(latestMessage).contains(EventType.ADJUSTMENT_CREATED.value)
      assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
      assertThat(latestMessage).contains("\\\"lastEvent\\\":true")
    }

    @Test
    fun createMany() {
      postCreateAdjustments(
        listOf(
          CREATED_ADJUSTMENT.copy(),
          CREATED_ADJUSTMENT.copy(
            fromDate = CREATED_ADJUSTMENT.fromDate!!.minusYears(1),
            toDate = CREATED_ADJUSTMENT.toDate!!.minusYears(1),
          ),
        ),
      )

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 2 }
      val messages = getLatestMessage()!!.messages()
      val first = messages.find { it.body().contains("\\\"lastEvent\\\":false") }
      assertThat(first).isNotNull
      val second = messages.find { it.body().contains("\\\"lastEvent\\\":true") }
      assertThat(second).isNotNull
    }

    @Test
    fun get() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }
      val result = webTestClient
        .get()
        .uri("/adjustments/$id")
        .headers(
          setViewAdjustmentsAuth(),
        )
        .exchange()
        .expectStatus().isOk
        .returnResult(AdjustmentDto::class.java)
        .responseBody.blockFirst()!!

      assertThat(result)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate", "createdDate")
        .isEqualTo(
          CREATED_ADJUSTMENT.copy(
            id = id,
            effectiveDays = 4,
            lastUpdatedBy = "Test User",
            status = ACTIVE,
            sentenceSequence = 1,
            adjustmentTypeText = CREATED_ADJUSTMENT.adjustmentType.text,
            days = 4,
            prisonId = "LDS",
            prisonName = "Leeds",
            adjustmentArithmeticType = CREATED_ADJUSTMENT.adjustmentType.arithmeticType,
            source = AdjustmentSource.DPS,
          ),
        )
      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    }

    @Test
    @Transactional
    fun update() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }
      val newFromDate = CREATED_ADJUSTMENT.fromDate!!.minusYears(1)
      val newToDate = CREATED_ADJUSTMENT.toDate!!.minusYears(1)
      putAdjustmentUpdate(
        id,
        CREATED_ADJUSTMENT.copy(
          fromDate = newFromDate,
          toDate = newToDate,
        ),
      )

      val adjustment = adjustmentRepository.findById(id).get()

      assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
      assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
      assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.UPDATE)
      assertThat(adjustment.adjustmentHistory[1].changeByUsername).isEqualTo("Test User")
      assertThat(adjustment.adjustmentHistory[1].changeSource).isEqualTo(AdjustmentSource.DPS)
      assertThat(adjustment.source).isEqualTo(AdjustmentSource.DPS)
      assertThat(adjustment.status).isEqualTo(ACTIVE)

      assertThat(adjustment.fromDate).isEqualTo(newFromDate)
      assertThat(adjustment.toDate).isEqualTo(newToDate)
      assertThat(adjustment.days).isNull()
      assertThat(adjustment.daysCalculated).isEqualTo(AdjustmentsTransactionalService.daysBetween(newFromDate, newToDate))
      assertThat(adjustment.effectiveDays).isEqualTo(AdjustmentsTransactionalService.daysBetween(newFromDate, newToDate))

      val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
      assertThat(legacyData).isEqualTo(
        LegacyData(
          bookingId = PrisonApiExtension.BOOKING_ID,
          sentenceSequence = 1,
          postedDate = LocalDate.now(),
          comment = null,
          type = null,
          chargeIds = listOf(9991),
        ),
      )

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
      val latestMessage: String = getLatestMessage()!!.messages()[0].body()
      assertThat(latestMessage).contains(adjustment.id.toString())
      assertThat(latestMessage).contains(EventType.ADJUSTMENT_UPDATED.value)
      assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
      assertThat(latestMessage).contains("\\\"lastEvent\\\":true")
    }
    @Test
    @Transactional
    fun updateEffectiveDays() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }
      postAdjustmentEffectiveDaysUpdate(
        id,
        AdjustmentEffectiveDaysDto(
          id,
          2,
          CREATED_ADJUSTMENT.person,
        ),
      )

      val adjustment = adjustmentRepository.findById(id).get()

      assertThat(adjustment.daysCalculated).isEqualTo(4)
      assertThat(adjustment.effectiveDays).isEqualTo(2)
      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
      val latestMessage: String = getLatestMessage()!!.messages()[0].body()
      assertThat(latestMessage).contains(adjustment.id.toString())
      assertThat(latestMessage).contains(EventType.ADJUSTMENT_UPDATED.value)
      assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
      assertThat(latestMessage).contains("\\\"unusedDeductions\\\":true")
      assertThat(latestMessage).contains("\\\"lastEvent\\\":true")
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-unused-deduction-adjustments.sql",
    )
    fun setManualUnusedDeductions() {
      CalculateReleaseDatesApiExtension.calculateReleaseDatesApi.stubCalculateUnusedDeductionsCouldNotCalculate()
      postSetManualUnusedDeductions(
        UNUSED_DEDUCTIONS_PRISONER_ID,
        ManualUnusedDeductionsDto(
          50,
        ),
      )

      val adjustments = adjustmentRepository.findByPerson(UNUSED_DEDUCTIONS_PRISONER_ID)
      val remand = adjustments.find { it.id.toString() == REMAND_ID }!!
      val taggedBail = adjustments.find { it.id.toString() == TAGGED_BAIL_ID }!!
      val unusedDeductions = adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }

      assertThat(remand.daysCalculated).isEqualTo(100)
      assertThat(remand.effectiveDays).isEqualTo(50)

      assertThat(taggedBail.days).isEqualTo(100)
      assertThat(taggedBail.effectiveDays).isEqualTo(100)

      assertThat(unusedDeductions).isNotNull
      assertThat(unusedDeductions!!.days).isEqualTo(50)

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 2 }
    }

    @Test
    fun `update with different adjustment type`() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }

      val result = webTestClient
        .put()
        .uri("/adjustments/$id")
        .headers(
          setAdjustmentsMaintainerAuth(),
        )
        .bodyValue(
          CREATED_ADJUSTMENT.copy(
            adjustmentType = UNLAWFULLY_AT_LARGE,
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .returnResult(ErrorResponse::class.java)
        .responseBody.blockFirst()!!
      assertThat(result.userMessage).isEqualTo("The provided adjustment type UNLAWFULLY_AT_LARGE doesn't match the persisted type REMAND")
      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    }

    @Test
    @Transactional
    fun delete() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }
      webTestClient
        .delete()
        .uri("/adjustments/$id")
        .headers(
          setAdjustmentsMaintainerAuth(),
        )
        .exchange()
        .expectStatus().isOk

      val adjustment = adjustmentRepository.findById(id).get()

      assertThat(adjustment.status).isEqualTo(DELETED)
      assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
      assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

      webTestClient
        .get()
        .uri("/adjustments/$id")
        .headers(
          setAdjustmentsMaintainerAuth(),
        )
        .exchange()
        .expectStatus().isNotFound

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
      val latestMessage: String = getLatestMessage()!!.messages()[0].body()
      assertThat(latestMessage).contains(adjustment.id.toString())
      assertThat(latestMessage).contains(EventType.ADJUSTMENT_DELETED.value)
      assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
      assertThat(latestMessage).contains("\\\"lastEvent\\\":true")
    }
  }

  @Nested
  inner class RemandTests {
    @Test
    fun `Create remand adjustment where charge ids do not exist`() {
      webTestClient
        .post()
        .uri("/adjustments")
        .headers(setAdjustmentsMaintainerAuth())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(listOf(CREATED_ADJUSTMENT.copy(remand = RemandDto(listOf(98765432)))))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class FindByTests {
    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
    )
    fun findByPerson() {
      val id = createAnAdjustment().also {
        cleanQueue()
      }
      val result = webTestClient
        .get()
        .uri("/adjustments?person=${PrisonApiExtension.PRISONER_ID}")
        .headers(
          setAdjustmentsMaintainerAuth(),
        )
        .exchange()
        .expectStatus().isOk
        .expectBodyList<AdjustmentDto>()
        .returnResult()
        .responseBody!!

      assertThat(result.size).isEqualTo(1)
      assertThat(result[0])
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate", "createdDate")
        .isEqualTo(
          CREATED_ADJUSTMENT.copy(
            id = id,
            person = PrisonApiExtension.PRISONER_ID,
            effectiveDays = 4,
            lastUpdatedBy = "Test User",
            status = ACTIVE,
            sentenceSequence = 1,
            adjustmentTypeText = CREATED_ADJUSTMENT.adjustmentType.text,
            days = 4,
            prisonId = "LDS",
            prisonName = "Leeds",
            adjustmentArithmeticType = CREATED_ADJUSTMENT.adjustmentType.arithmeticType,
            source = AdjustmentSource.DPS,
          ),
        )

      awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    }
  }

  @Nested
  inner class AdaTests {
    @Test
    @Transactional
    fun adaAdjustments() {
      val earliestSentenceDate = LocalDate.parse(PrisonApiExtension.EARLIEST_SENTENCE_DATE, DateTimeFormatter.ISO_LOCAL_DATE)
      val beforeEarliestSentence = earliestSentenceDate.minusDays(1)
      val createDto = CREATED_ADJUSTMENT.copy(
        fromDate = beforeEarliestSentence,
        person = PrisonApiExtension.PRISONER_ID,
        adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
        additionalDaysAwarded = AdditionalDaysAwardedDto(
          adjudicationId = listOf("987654321", "23456789"),
          prospective = true,
        ),
      )
      val adjustmentId = postCreateAdjustments(listOf(createDto))[0]

      val adjustment = adjustmentRepository.findById(adjustmentId).get()
      assertThat(adjustment.additionalDaysAwarded!!.adjudicationCharges).containsAll(
        listOf(
          AdjudicationCharges(
            "987654321",
          ),
          AdjudicationCharges("23456789"),
        ),
      )
      assertThat(adjustment.additionalDaysAwarded!!.prospective).isTrue

      var adjustmentDto = getAdjustmentById(adjustmentId)
      assertThat(adjustmentDto.additionalDaysAwarded).isEqualTo(
        AdditionalDaysAwardedDto(
          listOf("987654321", "23456789"),
          true,
        ),
      )

      // Assert that prospective adas before the earliest sentence date are included.
      var adjustments = getAdjustmentsByPerson(PrisonApiExtension.PRISONER_ID)
      assertThat(adjustments.contains(adjustmentDto)).isTrue

      val updateDto = CREATED_ADJUSTMENT.copy(
        fromDate = beforeEarliestSentence,
        id = adjustmentId,
        person = PrisonApiExtension.PRISONER_ID,
        adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
        additionalDaysAwarded = AdditionalDaysAwardedDto(
          adjudicationId = listOf("32415555"),
          prospective = false,
        ),
      )
      putAdjustmentUpdate(adjustmentId, updateDto)

      adjustmentDto = getAdjustmentById(adjustmentId)
      assertThat(adjustmentDto.additionalDaysAwarded).isEqualTo(AdditionalDaysAwardedDto(listOf("32415555"), false))

      // Assert that non-prospective adas before the earliest sentence date are not included.
      adjustments = getAdjustmentsByPerson(PrisonApiExtension.PRISONER_ID, startOfSentenceEnvelope = earliestSentenceDate)
      assertThat(adjustments.contains(adjustmentDto)).isFalse

      entityManager.refresh(adjustment)
      assertThat(adjustment.additionalDaysAwarded!!.adjudicationCharges).containsAll(listOf(AdjudicationCharges("32415555")))
      assertThat(adjustment.additionalDaysAwarded!!.prospective).isFalse
    }
  }

  @Nested
  inner class TaggedBailTests {

    @Test
    fun `Create tagged-bail where case sequence does not exist - bad request`() {
      webTestClient
        .post()
        .uri("/adjustments")
        .headers(setAdjustmentsMaintainerAuth())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          listOf(
            CREATED_ADJUSTMENT.copy(
              toDate = null,
              fromDate = null,
              days = 987,
              adjustmentType = TAGGED_BAIL,
              taggedBail = TaggedBailDto(caseSequence = 99995),
              remand = null,
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Create a Tagged Bail Adjustment, then update it`() {
      val adjustmentId = postCreateAdjustments(
        listOf(
          CREATED_ADJUSTMENT.copy(
            fromDate = null,
            toDate = null,
            days = 987,
            adjustmentType = TAGGED_BAIL,
            taggedBail = TaggedBailDto(caseSequence = 9191),
            remand = null,
            sentenceSequence = null,
          ),
        ),
      )[0]

      val createdAdjustment = getAdjustmentById(adjustmentId)

      assertThat(createdAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate", "createdDate")
        .isEqualTo(
          CREATED_ADJUSTMENT.copy(
            id = adjustmentId,
            fromDate = null,
            toDate = null,
            adjustmentType = TAGGED_BAIL,
            taggedBail = TaggedBailDto(caseSequence = 9191),
            effectiveDays = 987,
            remand = null,
            lastUpdatedBy = "Test User",
            status = ACTIVE,
            sentenceSequence = 1,
            adjustmentTypeText = TAGGED_BAIL.text,
            days = 987,
            prisonId = "LDS",
            prisonName = "Leeds",
            adjustmentArithmeticType = TAGGED_BAIL.arithmeticType,
            source = AdjustmentSource.DPS,
          ),
        )

      val updateDto = createdAdjustment.copy(days = 986)
      putAdjustmentUpdate(adjustmentId, updateDto)
      val updatedAdjustment = getAdjustmentById(adjustmentId)
      assertThat(updatedAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate")
        .isEqualTo(createdAdjustment.copy(effectiveDays = 986, days = 986))
    }
  }

  @Nested
  inner class UalTests {

    @Test
    fun `Create a UAL Adjustment, then update it`() {
      val adjustmentId = postCreateAdjustments(
        listOf(
          CREATED_ADJUSTMENT.copy(
            adjustmentType = UNLAWFULLY_AT_LARGE,
            unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
            remand = null,
          ),
        ),
      )[0]

      val adjustment = adjustmentRepository.findById(adjustmentId).get()

      assertThat(adjustment.unlawfullyAtLarge).isNotNull
      assertThat(adjustment.unlawfullyAtLarge!!.type).isEqualTo(RECALL)
      assertThat(adjustment.unlawfullyAtLarge!!.adjustmentId).isEqualTo(adjustmentId)

      val createdAdjustment = getAdjustmentById(adjustmentId)

      assertThat(createdAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate", "createdDate")
        .isEqualTo(
          CREATED_ADJUSTMENT.copy(
            id = adjustmentId,
            adjustmentType = UNLAWFULLY_AT_LARGE,
            unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
            remand = null,
            effectiveDays = 4,
            lastUpdatedBy = "Test User",
            status = ACTIVE,
            adjustmentTypeText = UNLAWFULLY_AT_LARGE.text,
            days = 4,
            prisonId = "LDS",
            prisonName = "Leeds",
            adjustmentArithmeticType = UNLAWFULLY_AT_LARGE.arithmeticType,
            source = AdjustmentSource.DPS,
          ),
        )

      val updateDto = createdAdjustment.copy(days = null, unlawfullyAtLarge = UnlawfullyAtLargeDto(type = ESCAPE))
      putAdjustmentUpdate(adjustmentId, updateDto)
      val updatedAdjustment = getAdjustmentById(adjustmentId)
      assertThat(updatedAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate")
        .isEqualTo(createdAdjustment.copy(unlawfullyAtLarge = UnlawfullyAtLargeDto(type = ESCAPE)))
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-nomis-ual.sql",
    )
    fun `Update a UAL Adjustment that has no UAL type (eg migrated from NOMIS) - update the type`() {
      val adjustmentId = UUID.fromString("dfba24ef-a2d4-4b26-af63-4d9494dd5252")
      val adjustment = getAdjustmentById(adjustmentId)

      putAdjustmentUpdate(
        adjustment.id!!,
        adjustment.copy(unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL), days = null),
      )

      val updatedAdjustment = getAdjustmentById(adjustmentId)
      assertThat(updatedAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate")
        .isEqualTo(
          adjustment.copy(
            lastUpdatedBy = "Test User",
            unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
            source = AdjustmentSource.DPS,
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-adjustments-spanning-sentence-envelope.sql",
    )
    fun `Get adjustments by person where some have been deleted, and some fall outside of the sentence envelope`() {
      val person = "BCDEFG"
      val result = getAdjustmentsByPerson(person, startOfSentenceEnvelope = LocalDate.of(2015, 3, 17))

      assertThat(result.map { it.lastUpdatedBy })
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(listOf("current-ual", "current-rada", "tagged-bail-no-dates", "remand-before-sentence"))
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-adjustments-spanning-sentence-envelope.sql",
    )
    fun `Get adjustments by person filter for adjustments before sentence envelope`() {
      val person = "BCDEFG"
      val result = getAdjustmentsByPerson(person, startOfSentenceEnvelope = LocalDate.of(2000, 1, 1))

      assertThat(result.map { it.lastUpdatedBy })
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(
          listOf(
            "current-ual",
            "current-rada",
            "tagged-bail-no-dates",
            "remand-before-sentence",
            "expired-ual",
            "expired-rada",
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-adjustments-spanning-sentence-envelope.sql",
    )
    fun `Get adjustments by person filter for adjustments without envelope filter`() {
      val person = "BCDEFG"
      val result = getAdjustmentsByPerson(person)

      assertThat(result.map { it.lastUpdatedBy })
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(
          listOf(
            "current-ual",
            "current-rada",
            "tagged-bail-no-dates",
            "remand-before-sentence",
            "expired-ual",
            "expired-rada",
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-adjustments-spanning-sentence-envelope.sql",
    )
    fun `Get adjustments by person filter for deleted adjustments`() {
      val person = "BCDEFG"
      val result = getAdjustmentsByPerson(person, status = DELETED, startOfSentenceEnvelope = LocalDate.of(2015, 3, 17))

      assertThat(result.map { it.lastUpdatedBy })
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(listOf("deleted-ual"))
    }

    @Test
    @Sql(
      "classpath:test_data/reset-data.sql",
      "classpath:test_data/insert-adjustment-with-prison.sql",
    )
    fun `Get adjustment details where a prison is associated)`() {
      val adjustmentId = UUID.fromString("dfba24ef-a2d4-4b26-af63-4d9494dd5252")
      val adjustment = getAdjustmentById(adjustmentId)

      putAdjustmentUpdate(
        adjustment.id!!,
        adjustment.copy(days = null, unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL)),
      )

      val updatedAdjustment = getAdjustmentById(adjustmentId)
      assertThat(updatedAdjustment)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("lastUpdatedDate")
        .isEqualTo(
          adjustment.copy(
            lastUpdatedBy = "Test User",
            unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
            prisonId = "LDS",
            prisonName = "Leeds",
            source = AdjustmentSource.DPS,
          ),
        )
    }
  }

  private fun getAdjustmentsByPerson(
    person: String,
    status: AdjustmentStatus? = null,
    startOfSentenceEnvelope: LocalDate? = null,
  ): List<AdjustmentDto> =
    webTestClient
      .get()
      .uri("/adjustments?person=$person${if (status != null) "&status=$status" else ""}${if (startOfSentenceEnvelope != null) "&sentenceEnvelopeDate=$startOfSentenceEnvelope" else ""}")
      .headers(setAdjustmentsMaintainerAuth())
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody

  private fun postCreateAdjustments(adjustmentDtos: List<AdjustmentDto>) = webTestClient
    .post()
    .uri("/adjustments")
    .headers(setAdjustmentsMaintainerAuth())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(adjustmentDtos)
    .exchange()
    .expectStatus().isCreated
    .returnResult(CreateResponseDto::class.java)
    .responseBody.blockFirst()!!.adjustmentIds

  private fun getAdjustmentById(adjustmentId: UUID) = webTestClient
    .get()
    .uri("/adjustments/$adjustmentId")
    .headers(setAdjustmentsMaintainerAuth())
    .exchange()
    .expectStatus().isOk
    .returnResult(AdjustmentDto::class.java)
    .responseBody.blockFirst()!!

  private fun createAnAdjustment(): UUID {
    return webTestClient
      .post()
      .uri("/adjustments")
      .headers(
        setAdjustmentsMaintainerAuth(),
      )
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(listOf(CREATED_ADJUSTMENT.copy()))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateResponseDto::class.java)
      .responseBody.blockFirst()!!.adjustmentIds[0]
  }

  @Test
  fun validate() {
    val validationMessages = webTestClient
      .post()
      .uri("/adjustments/validate")
      .headers(
        setAdjustmentsMaintainerAuth(),
      )
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          fromDate = LocalDate.now().plusYears(1),
          toDate = null,
          days = 25,
          bookingId = PrisonApiExtension.BOOKING_ID,
          adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(object : ParameterizedTypeReference<List<ValidationMessage>>() {})
      .returnResult().responseBody!!

    assertThat(validationMessages.size).isEqualTo(2)
    assertThat(validationMessages[0]).isEqualTo(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS))
    assertThat(validationMessages[1]).isEqualTo(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE))
  }

  private fun putAdjustmentUpdate(
    adjustmentId: UUID,
    updateDto: AdjustmentDto,
  ) {
    webTestClient
      .put()
      .uri("/adjustments/$adjustmentId")
      .headers(
        setAdjustmentsMaintainerAuth(),
      )
      .bodyValue(
        updateDto,
      )
      .exchange()
      .expectStatus().isOk
  }

  private fun postSetManualUnusedDeductions(
    person: String,
    days: ManualUnusedDeductionsDto,
  ) {
    webTestClient
      .post()
      .uri("/adjustments/person/$person/manual-unused-deductions")
      .headers(
        setAdjustmentsMaintainerAuth(),
      )
      .bodyValue(
        days,
      )
      .exchange()
      .expectStatus().isOk
  }

  companion object {
    private val CREATED_ADJUSTMENT = AdjustmentDto(
      id = null,
      bookingId = PrisonApiExtension.BOOKING_ID,
      person = PrisonApiExtension.PRISONER_ID,
      adjustmentType = AdjustmentType.REMAND,
      toDate = LocalDate.now().minusDays(2),
      fromDate = LocalDate.now().minusDays(5),
      days = null,
      additionalDaysAwarded = null,
      unlawfullyAtLarge = null,
      remand = RemandDto(chargeId = listOf(9991)),
      taggedBail = null,
    )
  }
}
