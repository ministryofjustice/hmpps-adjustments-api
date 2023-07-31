package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import java.util.UUID

@Entity
@Table
data class UnlawfullyAtLarge(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),
  @Enumerated(EnumType.STRING)
  var type: UnlawfullyAtLargeType? = null,

  @NotNull
  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "adjustmentId", nullable = false, updatable = false)
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),
)
