package uk.gov.justice.digital.hmpps.adjustments.api.config

import org.springframework.stereotype.Component

@Component
object UserContext {
  private val authToken = ThreadLocal<String>()
  private val activeCaseloadId = ThreadLocal<String?>()
  fun setAuthToken(token: String?) = authToken.set(token)
  fun getAuthToken(): String = authToken.get()
  fun setActiveCaseloadId(caseloadId: String?) = activeCaseloadId.set(caseloadId)
  fun getActiveCaseloadId(): String? = activeCaseloadId.get()
}
