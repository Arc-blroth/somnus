package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.LongOption
import ai.arcblroth.somnus3.commands.StringOption
import ai.arcblroth.somnus3.data.CounterData
import ai.arcblroth.somnus3.data.CounterDataTable
import ai.arcblroth.somnus3.data.withCounterData
import dev.kord.core.Kord
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun CommandRegistry.registerCounterCommands(
    kord: Kord,
    config: Config,
) {
    slashModifyCounter(
        name = "add",
        aliases = arrayOf("++"),
        description = "Increments a counter.",
        defaultValue = 1L,
        modifier = { a, b -> a + b },
        actionInfinitive = "increment",
        actionPastParticiple = "incremented",
    )

    slashModifyCounter(
        name = "subtract",
        aliases = arrayOf("sub", "--"),
        description = "Decrements a counter.",
        defaultValue = 1L,
        modifier = { a, b -> a - b },
        actionInfinitive = "decrement",
        actionPastParticiple = "decremented",
    )

    slashModifyCounter(
        name = "set",
        description = "Sets a counter.",
        defaultValue = null,
        modifier = { _, b -> b },
        actionInfinitive = "set",
        actionPastParticiple = "set",
    )

    slash("remove") {
        description = "Deletes a counter."
        options =
            listOf(
                StringOption(
                    name = "name",
                    description = "Name of the counter to yeet forever!",
                    onParseFailure = { content = "Invalid counter specified!" },
                ),
            )
        execute = { _, _, options ->
            val counterName = options["name"] as String
            transaction {
                val counter = CounterData.find { CounterDataTable.name eq counterName }.singleOrNull()
                if (counter == null) {
                    respond {
                        content = "That counter doesn't exist!"
                    }
                } else {
                    counter.delete()
                    respond {
                        content = "Counter `$counterName` with value **${counter.counter}** deleted."
                    }
                }
            }
        }
    }
}

private fun CommandRegistry.slashModifyCounter(
    name: String,
    aliases: Array<String> = arrayOf(),
    description: String,
    defaultValue: Long?,
    modifier: (Long, Long) -> Long,
    actionInfinitive: String,
    actionPastParticiple: String,
) {
    slash(name, *aliases) {
        this.description = description
        options =
            listOf(
                StringOption(
                    name = "name",
                    description = "Name of the counter to $actionInfinitive.",
                    onParseFailure = { content = "Invalid counter specified!" },
                ),
                LongOption(
                    name = "value",
                    description = "Value to $actionInfinitive.",
                    optional = defaultValue != null,
                    onParseFailure = { content = "`$it` is not a number!" },
                ),
            )
        execute = { _, _, options ->
            val counterName = options["name"] as String
            val value = (options["value"] as Long? ?: defaultValue)!!
            if (counterName.length > 1900) {
                respond {
                    content = "Counter name too long!"
                }
            } else {
                withCounterData(counterName) {
                    val original = counter
                    counter = modifier(counter, value)
                    respond {
                        content = "Counter **$counterName** was $actionPastParticiple from $original to **$counter**"
                    }
                }
            }
        }
    }
}
