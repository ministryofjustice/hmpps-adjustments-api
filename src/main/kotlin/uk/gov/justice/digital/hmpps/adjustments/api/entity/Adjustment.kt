package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.adjustments.api.config.PostgreSQLEnumType
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
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

  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,

  @NotNull
  var deleted: Boolean = false,

  @OneToMany(mappedBy = "adjustment", cascade = [CascadeType.ALL])
  var adjustmentHistory: List<AdjustmentHistory> = ArrayList(),

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "source_systems")
  @Type(PostgreSQLEnumType::class)
  var source: AdjustmentSource = AdjustmentSource.DPS,

) {

  init {
    adjustmentHistory.forEach { it.adjustment = this }
  }
}
