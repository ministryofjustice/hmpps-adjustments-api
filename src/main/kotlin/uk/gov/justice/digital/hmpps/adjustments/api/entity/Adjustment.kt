package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonType
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDate
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table
@TypeDefs(
  TypeDef(name = "json", typeClass = JsonType::class)
)
data class Adjustment(

  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  val person: String = "",

  var fromDate: LocalDate? = null,

  var toDate: LocalDate? = null,

  var days: Int? = null,

  @NotNull
  var daysCalculated: Int = -1,

  @NotNull
  @Enumerated(EnumType.STRING)
  val adjustmentType: AdjustmentType = AdjustmentType.REMAND,

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = JacksonUtil.toJsonNode("{}"),

  @NotNull
  var deleted: Boolean = false,

  @OneToMany(mappedBy = "adjustment", cascade = [CascadeType.ALL])
  var adjustmentHistory: List<AdjustmentHistory> = ArrayList(),

  @NotNull
  @Enumerated(EnumType.STRING)
  var source: AdjustmentSource = AdjustmentSource.DPS

) {

  init {
    adjustmentHistory.forEach { it.adjustment = this }
  }
}
