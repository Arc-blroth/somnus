package ai.arcblroth.somnus3.data

import org.jetbrains.exposed.dao.id.IntIdTable

open class PlayerSnowflakeIdTable : SnowflakeIdTable(columnName = "playerSnowflake") {
    val playerSnowflake by ::id
}

object PlayerDataTable : PlayerSnowflakeIdTable() {
    val lastDailyRewardTime = long("lastDailyRewardTime")
    val sleepPoints = integer("sleepPoints")
    val moneyPoints = integer("moneyPoints")
    val knowledgePoints = integer("knowledgePoints")
    val hitPoints = integer("hitPoints")
    val swagPoints = integer("swagPoints")
    val gamePoints = integer("gamePoints")
    val furryPoints = double("furryPoints")
    val bedType = enumeration("bedType", BedType::class)
}

object PreferencesDataTable : PlayerSnowflakeIdTable() {
    val showDeathMessages = bool("showDeathMessages").default(true)
}

object CounterDataTable : IntIdTable() {
    // technically `name` should never exceed 4000 characters, but for
    // backwards compatibility we use TEXT rather than VARCHAR here
    val name = text("name")
    val counter = long("counter")
}
