package ai.arcblroth.somnus3.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths

private const val DB_FILE = ".data/sqlite.db"

fun initDatabase() {
    val databaseFile = Paths.get(System.getProperty("user.dir"), DB_FILE).toFile()
    if (!databaseFile.exists()) {
        databaseFile.parentFile.mkdirs()
        databaseFile.createNewFile()
    }
    Database.connect("jdbc:sqlite:$DB_FILE", databaseConfig = DatabaseConfig { useNestedTransactions = true })
    transaction {
        SchemaUtils.create(PlayerDataTable, PreferencesDataTable, CounterDataTable)
    }
    println("PlayerData and CounterData tables created!")
}
