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
fun <T> withPlayerData(player: Snowflake, block: PlayerData.() -> T) =
    withData(PlayerData, PlayerDataTable, player, PlayerData::initPlayerData, block)

fun <T> withAngelData(player: Snowflake, block: AngelData.() -> T) =
    withData(AngelData, AngelDataTable, player, {}, block)

fun <T> withPreferencesData(player: Snowflake, block: PreferencesData.() -> T) =
    withData(PreferencesData, PreferencesDataTable, player, { showDeathMessages = true }, block)

fun <T> withCounterData(counterName: String, block: CounterData.() -> T) =
    withData(CounterData, CounterDataTable, null, { name = counterName; counter = 0 }, block, { it.name eq counterName })

/**
 * Opens a database transaction to `UPDATE` some data type.
 * If existing data doesn't exist in the database, new data will be inserted.
 */
private inline fun <reified Key, reified Table, reified DataType, reified DataTypeClass, Output> withData(
    dataTypeClass: DataTypeClass,
    table: Table,
    key: Key?,
    crossinline defaultBuilder: DataType.() -> Unit,
    noinline block: DataType.() -> Output,
    crossinline finder: SqlExpressionBuilder.(Table) -> Op<Boolean> = { it.id eq key },
): Output
    where Key : Comparable<Key>,
          Table : IdTable<Key>,
          DataType : Entity<Key>,
          DataTypeClass : EntityClass<Key, DataType> =
    transaction {
        val data = dataTypeClass.find { finder(table) }.singleOrNull()

        @Suppress("IfThenToElvis")
        return@transaction if (data == null) {
            var output: Output? = null
            dataTypeClass.new(key) {
                defaultBuilder()
                output = block()
            }
            output!!
        } else {
            data.block()
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
