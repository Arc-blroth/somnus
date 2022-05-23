package ai.arcblroth.somnus3.data

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PlayerData(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
    companion object : SnowflakeEntityClass<PlayerData>(PlayerDataTable)

    var lastDailyRewardTime by PlayerDataTable.lastDailyRewardTime
    var sleepPoints by PlayerDataTable.sleepPoints
    var moneyPoints by PlayerDataTable.moneyPoints
    var knowledgePoints by PlayerDataTable.knowledgePoints
    var hitPoints by PlayerDataTable.hitPoints
    var swagPoints by PlayerDataTable.swagPoints
    var gamePoints by PlayerDataTable.gamePoints
    var furryPoints by PlayerDataTable.furryPoints
    var bedType by PlayerDataTable.bedType
}

class PreferencesData(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
    companion object : SnowflakeEntityClass<PreferencesData>(PreferencesDataTable)

    var showDeathMessages by PreferencesDataTable.showDeathMessages
}

class CounterData(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CounterData>(CounterDataTable)

    var name by CounterDataTable.name
    var counter by CounterDataTable.counter
}
