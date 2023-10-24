package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Table
@Entity
data class AdditionalDaysAwarded(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),
  @NotNull
  var prospective: Boolean = false,

  @ElementCollection
  @CollectionTable(name = "adjudicationCharges", joinColumns = [JoinColumn(name = "adjustment_id")])
  var adjudicationCharges: MutableList<AdjudicationCharges> = mutableListOf(),

  @OneToOne
  @MapsId
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),
)
