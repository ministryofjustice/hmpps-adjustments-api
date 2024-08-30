package uk.gov.justice.digital.hmpps.adjustments.api.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
@NamedEntityGraph(
  name = "Adjustment.detail",
  attributeNodes = [
    NamedAttributeNode("adjustmentHistory"),
    NamedAttributeNode("additionalDaysAwarded"),
    NamedAttributeNode("unlawfullyAtLarge")
  ],
)
data class Adjustment(

  @Id
  @NotNull
  val id: UUID = UUID.randomUUID(),

  @NotNull
  var person: String = "",

  var fromDate: LocalDate? = null,

  var toDate: LocalDate? = null,

  /* Days specified by the user. */
  var days: Int? = null,

  /* Days between the from and to dates of the adjustment. */
  var daysCalculated: Int? = null,

  /* Days effective in a calculation. For example remand minus unused days. (NOMIS always records effective days.) */
  @NotNull
  var effectiveDays: Int = -1,

  @NotNull
  @Enumerated(EnumType.STRING)
  var adjustmentType: AdjustmentType = AdjustmentType.REMAND,

  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,

  @NotNull
  @Enumerated(EnumType.STRING)
  var status: AdjustmentStatus = AdjustmentStatus.ACTIVE,

  @OneToMany(mappedBy = "adjustment", cascade = [CascadeType.ALL])
  @JsonIgnore
  @BatchSize(size = 1000)
  var adjustmentHistory: List<AdjustmentHistory> = ArrayList(),

  @NotNull
  @Enumerated(EnumType.STRING)
  var source: AdjustmentSource = AdjustmentSource.DPS,

  @OneToOne(mappedBy = "adjustment", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var additionalDaysAwarded: AdditionalDaysAwarded? = null,

  @OneToOne(mappedBy = "adjustment", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var unlawfullyAtLarge: UnlawfullyAtLarge? = null,

) {
  init {
    adjustmentHistory.forEach { it.adjustment = this }
    unlawfullyAtLarge?.adjustment = this
    additionalDaysAwarded?.adjustment = this
  }
}
