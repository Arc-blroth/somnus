package ai.arcblroth.somnus3.data

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
        SchemaUtils.create(PlayerDataTable, AngelDataTable, PreferencesDataTable, CounterDataTable)
    }
    println("PlayerData and CounterData tables created!")
}
