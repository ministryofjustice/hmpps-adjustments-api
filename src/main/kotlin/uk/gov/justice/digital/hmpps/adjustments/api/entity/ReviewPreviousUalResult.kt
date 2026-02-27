package uk.gov.justice.digital.hmpps.adjustments.api.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table
@Entity
data class ReviewPreviousUalResult(
  @Id
  val id: UUID,

  val adjustmentId: UUID,

  val person: String,

  @Enumerated(EnumType.STRING)
  val status: ReviewPreviousUalStatus,

  val reviewedByUsername: String,

  val reviewedByPrisonId: String,

  val reviewedAt: LocalDateTime,
) {
  companion object {
    fun rejected(adjustmentId: UUID, person: String, username: String, prison: String): ReviewPreviousUalResult = ReviewPreviousUalResult(
      id = UUID.randomUUID(),
      adjustmentId = adjustmentId,
      person = person,
      status = ReviewPreviousUalStatus.REJECTED,
      reviewedByUsername = username,
      reviewedByPrisonId = prison,
      reviewedAt = LocalDateTime.now(),
    )

    fun accepted(adjustmentId: UUID, person: String, username: String, prison: String): ReviewPreviousUalResult = ReviewPreviousUalResult(
      id = UUID.randomUUID(),
      adjustmentId = adjustmentId,
      person = person,
      status = ReviewPreviousUalStatus.ACCEPTED,
      reviewedByUsername = username,
      reviewedByPrisonId = prison,
      reviewedAt = LocalDateTime.now(),
    )
  }
}
