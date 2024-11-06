package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept

class ThingsToDoServiceTest {
  private val additionalDaysAwardedService = mock<AdditionalDaysAwardedService>()

  private val thingsToDoService = ThingsToDoService(additionalDaysAwardedService)

  @Nested
  inner class GetToDoListTests {

    @Test
    fun `Get things to do for a prisoner where there are things to do`() {
      whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)).thenReturn(
        getAdaAdjudicationDetails(PADA),
      )

      val thingsToDo = thingsToDoService.getToDoList(NOMS_ID)

      assertThat(thingsToDo).isEqualTo(ThingsToDo(prisonerId = NOMS_ID, thingsToDo = listOf(ADA_INTERCEPT)))
    }

    @Test
    fun `Get things to do for a prisoner when there is nothing to do`() {
      whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)).thenReturn(
        getAdaAdjudicationDetails(NONE),
      )

      val thingsToDo = thingsToDoService.getToDoList(NOMS_ID)

      assertThat(thingsToDo).isEqualTo(ThingsToDo(prisonerId = NOMS_ID))
    }
  }

  private fun getAdaAdjudicationDetails(type: InterceptType) = AdaAdjudicationDetails(
    intercept = AdaIntercept(
      type = type,
      number = 1,
      anyProspective = true,
      messageArguments = emptyList(),
    ),
  )

  companion object {
    const val NOMS_ID = "AA1234A"
  }
}
