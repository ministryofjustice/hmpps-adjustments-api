package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table
data class TimeSpentAsAnAppealApplicant(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),

  var courtOfAppealReferenceNumber: String? = null,

  @OneToOne
  @MapsId
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),
)
