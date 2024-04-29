package uk.gov.justice.digital.hmpps.adjustments.api.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class ProspectiveAdaRejection(

  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  val dateChargeProved: LocalDate? = null,

  @NotNull
  val person: String = "",

  @NotNull
  val days: Int = -1,

  @NotNull
  val rejectionAt: LocalDateTime = LocalDateTime.now(),

)
