package no.kartverket.nibas

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Ignore


class DBMigrationHelper {

    private val baselineFile = "src/main/resources/db/migration/V1__Administrative_og_statistiske_inndelinger.sql"

    @Ignore
    @Test
    fun createAlterTableStatements() {
        val allLines = Files.readAllLines(Paths.get(baselineFile))
        val resultList = mutableListOf<String>()
        allLines.forEachIndexed { index, line ->
            if (line.contains("oppdateringsdato")) {
                val subList = allLines.subList(0, index)
                subList.reverse()
                val table = getTable(subList)
                resultList.add("ALTER TABLE $table add column endret_av text;")
            }
        }
        resultList.forEach { println(it) }
    }

    @Ignore
    @Test
    fun createDropColumnStatements() {
        val allLines = Files.readAllLines(Paths.get(baselineFile))
        var resultList: List<String> = emptyList()
        allLines.forEachIndexed { index, line ->
//            navnerom
            val dropStatement = createDropStatementForColumn("foelgergrunnkretsgrense_navnerom", line, allLines, index)
            if (dropStatement != null) {
                resultList = resultList.plus(dropStatement)
            }
        }
        allLines.forEachIndexed { index, line ->
//            versjonid
            val dropStatement = createDropStatementForColumn("foelgergrunnkretsgrense_versjonid", line, allLines, index)
            if (dropStatement != null) {
                resultList = resultList.plus(dropStatement)
            }
        }
        println("ResultList size: ${resultList.size}")
        val sortedUniqueList = resultList.toSet().sorted()
        println("Sorted unique size: ${sortedUniqueList.size}")
        sortedUniqueList.forEach { println(it) }
    }

    private fun createDropStatementForColumn(
        columnName: String,
        line: String,
        allLines: List<String>,
        index: Int,
        columnNameToChange: String = columnName
    ): String? {
        if (!line.startsWith("--") && line.contains(" $columnName ")) {
            val subList = allLines.subList(0, index).toMutableList()
            subList.reverse()
            val table = getTable(subList)
            return "ALTER TABLE $table drop column $columnNameToChange;"
        }
        return null
    }

    private fun getTable(subList: MutableList<String>) = subList
        .first { !it.startsWith("--") && it.contains("CREATE TABLE") }
        .split(" ")[2]

}
