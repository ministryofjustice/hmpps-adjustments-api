package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
@Table
data class Remand(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),

  @OneToOne
  @MapsId
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),

  @NotNull
  @OneToOne
  @JoinColumn(name = "unusedRemandId")
  @JsonIgnore
  var unusedRemand: Adjustment = Adjustment(),
)
