package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.adjustments.api.enums.TimeSpentInCustodyAbroadDocumentationSource
import java.util.UUID

@Entity
@Table
data class TimeSpentInCustodyAbroad(
  @Id
  val adjustmentId: UUID = UUID.randomUUID(),
  @Enumerated(EnumType.STRING)
  var documentationSource: TimeSpentInCustodyAbroadDocumentationSource? = null,

  @OneToOne
  @MapsId
  @JsonIgnore
  var adjustment: Adjustment = Adjustment(),
)
