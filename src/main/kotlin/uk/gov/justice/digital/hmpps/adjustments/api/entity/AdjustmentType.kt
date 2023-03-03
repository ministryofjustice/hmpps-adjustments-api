package uk.gov.justice.digital.hmpps.adjustments.api.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table
data class AdjustmentType (

  @Id
  val id: String,

  @NotNull
  val creditOrDebit: Boolean
)