package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import com.vladmihalcea.hibernate.type.json.JsonType
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table
@TypeDefs(
  TypeDef(name = "json", typeClass = JsonType::class),
  TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType::class)
)
class AdjustmentHistory(

  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "adjustmentId", nullable = false, updatable = false)
  var adjustment: Adjustment = Adjustment(),

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "change_types")
  @Type(type = "pgsql_enum")
  val changeType: ChangeType = ChangeType.CREATE,

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  val change: JsonNode = JacksonUtil.toJsonNode("{}"),

  @NotNull
  val changeByUsername: String = "",

  @NotNull
  val changeAt: LocalDateTime = LocalDateTime.now(),

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "source_systems")
  @Type(type = "pgsql_enum")
  val changeSource: AdjustmentSource = AdjustmentSource.DPS
)
