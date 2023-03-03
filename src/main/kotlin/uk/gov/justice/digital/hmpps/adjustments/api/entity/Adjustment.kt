package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDate
import com.vladmihalcea.hibernate.type.json.JsonType
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table
@TypeDefs(
  TypeDef(name = "json", typeClass = JsonType::class)
)
data class Adjustment (

  @Id
  @NotNull
  @Type(type="org.hibernate.type.PostgresUUIDType")
  val id: UUID,

  @NotNull
  val person: String,

  @NotNull
  val fromDate: LocalDate,

  @NotNull
  val toDate: LocalDate,

  @NotNull
  val days: Int,

  @NotNull
  val daysCalculated: Int,

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "adjustmentTypeId", nullable = false, updatable = false)
  val adjustmentType: AdjustmentType,

  @NotNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  val legacyData: JsonNode,

  @NotNull
  val deleted: Boolean = false

  )