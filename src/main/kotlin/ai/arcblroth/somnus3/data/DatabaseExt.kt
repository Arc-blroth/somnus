package ai.arcblroth.somnus3.data

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Opens a database transaction to `UPDATE` the given player's data.
 * If the player doesn't exist in the database, new data will be inserted.
 */
fun withPlayerData(player: Snowflake, block: PlayerData.() -> Unit) =
    withData(PlayerData, PlayerDataTable, player, PlayerData::initPlayerData, block)

fun withPreferencesData(player: Snowflake, block: PreferencesData.() -> Unit) =
    withData(PreferencesData, PreferencesDataTable, player, { showDeathMessages = true }, block)

fun withCounterData(counterName: String, block: CounterData.() -> Unit) =
    withData(CounterData, CounterDataTable, null, { name = counterName; counter = 0 }, block, { it.name eq counterName })

/**
 * Opens a database transaction to `UPDATE` some data type.
 * If existing data doesn't exist in the database, new data will be inserted.
 */
private inline fun <reified Key, reified Table, reified DataType, reified DataTypeClass> withData(
    dataTypeClass: DataTypeClass,
    table: Table,
    key: Key?,
    crossinline defaultBuilder: DataType.() -> Unit,
    noinline block: DataType.() -> Unit,
    crossinline finder: SqlExpressionBuilder.(Table) -> Op<Boolean> = { it.id eq key },
)
    where Key : Comparable<Key>,
          Table : IdTable<Key>,
          DataType : Entity<Key>,
          DataTypeClass : EntityClass<Key, DataType> {
    transaction {
        val data = dataTypeClass.find { finder(table) }.singleOrNull()

        @Suppress("IfThenToElvis")
        if (data == null) {
            dataTypeClass.new(key) {
                defaultBuilder()
                block()
            }
        } else {
            data.block()
        }
    }
}

/**
 * Initial player data for new players.
 */
fun PlayerData.initPlayerData() {
    lastDailyRewardTime = Clock.System.now().toEpochMilliseconds()
    sleepPoints = 0
    moneyPoints = 100
    knowledgePoints = 0
    hitPoints = 20
    swagPoints = 0
    gamePoints = 0
    furryPoints = 0.0
    bedType = BedType.FLOOR
}
