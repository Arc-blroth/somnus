package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Constants
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.MessageCreateBuilder

typealias ParseFailureCallback = MessageCreateBuilder.(String) -> Unit

@SomnusCommandsDsl
sealed class Option<T>(
    val name: String,
    val description: String,
    val optional: Boolean = false,
    val onParseFailure: ParseFailureCallback? = null,
) {
    fun toOptionsBuilder(): OptionsBuilder = toOptionsBuilderInner().apply { required = !optional }

    protected abstract fun toOptionsBuilderInner(): OptionsBuilder

    abstract fun parse(token: String): T?
}

class StringOption(
    name: String,
    description: String,
    val choices: Map<String, String>? = null,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<String>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() =
        StringChoiceBuilder(name, description).also { builder ->
            this@StringOption.choices?.forEach {
                builder.choice(it.key, it.value)
            }
        }

    override fun parse(token: String) = if (choices != null && !choices.containsValue(token)) null else token
}

class MessageContentOption(
    name: String,
    description: String,
    optional: Boolean = false,
) : Option<String>(name, description, optional, onParseFailure = {}) {
    override fun toOptionsBuilderInner() = StringChoiceBuilder(name, description)

    override fun parse(token: String) = throw NotImplementedError("Handled as a special case in CommandRegistry")
}

class LongOption(
    name: String,
    description: String,
    val choices: Map<String, Long>? = null,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<Long>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() =
        IntegerOptionBuilder(name, description).also { builder ->
            this@LongOption.choices?.forEach {
                builder.choice(it.key, it.value)
            }
        }

    override fun parse(token: String): Long? {
        val long = token.toLongOrNull() ?: return null
        return if (choices != null && !choices.containsValue(long)) null else long
    }
}

class DoubleOption(
    name: String,
    description: String,
    val choices: Map<String, Double>? = null,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<Double>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() =
        NumberOptionBuilder(name, description).also { builder ->
            this@DoubleOption.choices?.forEach {
                builder.choice(it.key, it.value)
            }
        }

    override fun parse(token: String): Double? {
        val double = token.toDoubleOrNull() ?: return null
        return if (choices != null && !choices.containsValue(double)) null else double
    }
}

class BooleanOption(
    name: String,
    description: String,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback? = { content = "Toggle value should either be `true` or `false`." },
) : Option<Boolean>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() = BooleanBuilder(name, description)

    override fun parse(token: String) = token.toBooleanStrictOrNull()
}

class UserOption(
    name: String,
    description: String,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<Snowflake>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() = UserBuilder(name, description)

    override fun parse(token: String): Snowflake? {
        val groups = Constants.MENTION_FILTER.find(token)?.groups ?: return null
        // filter *out* role mentions
        return if (groups[1]!!.value == "&") {
            null
        } else {
            return Snowflake(groups[2]!!.value.toLongOrNull() ?: return null)
        }
    }
}

class RoleOption(
    name: String,
    description: String,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<Snowflake>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() = RoleBuilder(name, description)

    override fun parse(token: String): Snowflake? {
        val groups = Constants.MENTION_FILTER.find(token)?.groups ?: return null
        // filter *in* role mentions
        return if (groups[1]!!.value == "&") {
            Snowflake(groups[2]!!.value.toLongOrNull() ?: return null)
        } else {
            null
        }
    }
}

class ChannelOption(
    name: String,
    description: String,
    optional: Boolean = false,
    onParseFailure: ParseFailureCallback?,
) : Option<Snowflake>(name, description, optional, onParseFailure) {
    override fun toOptionsBuilderInner() = ChannelBuilder(name, description)

    override fun parse(token: String): Snowflake? {
        val groups = Constants.CHANNEL_FILTER.find(token)?.groups ?: return null
        return Snowflake(groups[1]!!.value.toLongOrNull() ?: return null)
    }
}
