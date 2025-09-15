package ai.arcblroth.somnus3.data

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

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

object AngelDataTable : PlayerSnowflakeIdTable() {
    val type = enumeration("type", AngelType::class).default(AngelType.LUNA)
    val digModifier = double("digModifier").default(1.0)
    val eatModifier = double("eatModifier").default(1.0)
    val learnModifier = double("learnModifier").default(1.0)
    val gameModifier = double("gameModifier").default(1.0)
    val worshipModifier = double("worshipModifier").default(1.0)
}

object PreferencesDataTable : PlayerSnowflakeIdTable() {
    val showDeathMessages = bool("showDeathMessages").default(false)
    val showWittyMessages = bool("showWittyMessages").default(true)
    val showKittyMessages = bool("showKittyMessages").default(false)
}

object CounterDataTable : IntIdTable() {
    // technically `name` should never exceed 4000 characters, but for
    // backwards compatibility we use TEXT rather than VARCHAR here
    val name = text("name")
    val counter = long("counter")
}
