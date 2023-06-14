package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
@Table
data class AdditionalDaysAwarded(

  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  var adjudicationId: String = "",

  @NotNull
  var consecutive: Boolean = false,

  @NotNull
  @OneToOne(optional = false)
  @JoinColumn(name = "adjustmentId", nullable = false, updatable = false)
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),
)
