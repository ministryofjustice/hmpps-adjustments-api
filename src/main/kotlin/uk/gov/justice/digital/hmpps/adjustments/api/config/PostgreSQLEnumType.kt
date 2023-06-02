package uk.gov.justice.digital.hmpps.adjustments.api.config

import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.type.EnumType
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Types

class PostgreSQLEnumType<E : Enum<E>> : EnumType<E>() {
  @Throws(HibernateException::class, SQLException::class)
  override fun nullSafeSet(st: PreparedStatement, value: E?, index: Int, session: SharedSessionContractImplementor) {
    if (value == null) {
      st.setNull(index, Types.OTHER)
    } else {
      st.setObject(
        index,
        value.toString(),
        Types.OTHER,
      )
    }
  }
}
