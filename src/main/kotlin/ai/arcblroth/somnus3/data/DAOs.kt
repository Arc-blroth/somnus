package ai.arcblroth.somnus3.data

import org.jetbrains.exposed.sql.Table

open class PlayerSnowflakeIdTable() : SnowflakeIdTable(columnName = "playerSnowflake") {
    val playerSnowflake by ::id
}

object PlayerData : PlayerSnowflakeIdTable() {
    val lastDailyRewardTime = long("lastDailyRewardTime")
    val sleepPoints = integer("sleepPoints")
    val moneyPoints = integer("moneyPoints")
    val knowledgePoints = integer("knowledgePoints")
    val hitPoints = integer("hitPoints")
    val swagPoints = integer("swagPoints")
    val gamePoints = integer("gamePoints")
}

object PreferencesData : PlayerSnowflakeIdTable() {
    val showDeathMessages = bool("showDeathMessages").default(true)
}

object CounterData : Table() {
    // technically `name` should never exceed 4000 characters, but for
    // backwards compatibility we use TEXT rather than VARCHAR here
    val name = text("name")
    val counter = long("counter")
}
