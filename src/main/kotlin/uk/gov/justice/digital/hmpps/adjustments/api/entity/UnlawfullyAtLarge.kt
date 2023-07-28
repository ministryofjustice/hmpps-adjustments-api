package uk.gov.justice.digital.hmpps.adjustments.api.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import java.util.UUID

@Entity
@Table
data class UnlawfullyAtLarge(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),
  @Enumerated(EnumType.STRING)
  val type: UnlawfullyAtLargeType,

  @OneToOne
  @MapsId
  var adjustment: Adjustment = Adjustment(),
)
