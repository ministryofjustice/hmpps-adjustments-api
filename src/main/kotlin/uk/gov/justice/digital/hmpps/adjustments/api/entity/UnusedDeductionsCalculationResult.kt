package uk.gov.justice.digital.hmpps.adjustments.api.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationStatus
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class UnusedDeductionsCalculationResult(
  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  var person: String = "",

  @NotNull
  val calculationAt: LocalDateTime = LocalDateTime.now(),

  @NotNull
  @Enumerated(EnumType.STRING)
  val status: UnusedDeductionsCalculationStatus,

)
