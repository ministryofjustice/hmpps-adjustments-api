package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table
data class AdjustmentHistory (

  @Id
  @NotNull
  @org.hibernate.annotations.Type(type="org.hibernate.type.PostgresUUIDType")
  val id: UUID,

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "adjustmentId", nullable = false, updatable = false)
  val adjustment: Adjustment,

  @NotNull
  @Enumerated(EnumType.STRING)
  val changeType: ChangeType,

  @NotNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  val change: JsonNode,

  @NotNull
  val changeByUsername: String,

  @NotNull
  val changeAt: LocalDateTime = LocalDateTime.now(),
  )