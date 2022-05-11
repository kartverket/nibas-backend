package no.kartverket.nibas

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Ignore

class DBMigrationHelper {

    @Ignore
    @Test
    fun createAlterTableStatements() {
        val allLines = Files.readAllLines(Paths.get("src/main/resources/db/migration/V1__Administrative_og_statistiske_inndelinger.sql"))
        val resultList = mutableListOf<String>()
        allLines.forEachIndexed { index, line ->
            if (line.contains("oppdateringsdato")) {
                val subList = allLines.subList(0, index)
                subList.reverse()
                val table = extractTableName(subList.first { it.contains("CREATE TABLE") })
                resultList.add("ALTER TABLE $table add column endret_av text;")
            }
        }
        resultList.forEach { println(it) }
    }

    private fun extractTableName(line: String): String {
        return line.split(" ")[2]
    }
}
