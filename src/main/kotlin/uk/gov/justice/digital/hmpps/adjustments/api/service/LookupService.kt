package uk.gov.justice.digital.hmpps.adjustments.api.service

import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.Ada
import java.time.LocalDate

interface LookupService {
  fun lookupAdas(nomsId: String, startOfSentenceEnvelope: LocalDate): List<Ada>
}
