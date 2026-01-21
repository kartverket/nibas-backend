package no.kartverket.nibas.service

import javax.sql.DataSource
import no.kartverket.nibas.common.TransactionScope
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@TransactionScope
class TransactionTimeService(private val dataSource: DataSource) {
    @get:Transactional(propagation = Propagation.MANDATORY)
    val transactionTime: Instant by lazy {
        val connection = DataSourceUtils.getConnection(dataSource)
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT CURRENT_TIMESTAMP").use { resultSet ->
                check(resultSet.next())
                resultSet.getTimestamp(1).toInstant()
            }
        }
    }
}
