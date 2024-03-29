package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
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
  val changeType: ChangeType = ChangeType.CREATE,

  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  val change: JsonNode = JacksonUtil.toJsonNode("{}"),

  @NotNull
  val changeByUsername: String = "",

  @NotNull
  val changeAt: LocalDateTime = LocalDateTime.now(),

  @NotNull
  @Enumerated(EnumType.STRING)
  val changeSource: AdjustmentSource = AdjustmentSource.DPS,

  val prisonId: String? = null,
)
