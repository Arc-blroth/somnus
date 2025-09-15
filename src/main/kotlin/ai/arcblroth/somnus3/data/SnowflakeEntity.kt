package ai.arcblroth.somnus3.data

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

typealias SnowflakeEntity = Entity<Snowflake>

abstract class SnowflakeEntityClass<out E : SnowflakeEntity>(
    table: IdTable<Snowflake>,
) : EntityClass<Snowflake, E>(table)
