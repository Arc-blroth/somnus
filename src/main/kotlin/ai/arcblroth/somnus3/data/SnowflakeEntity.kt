package ai.arcblroth.somnus3.data

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.IdTable

typealias SnowflakeEntity = Entity<Snowflake>

abstract class SnowflakeEntityClass<out E : SnowflakeEntity> constructor(table: IdTable<Snowflake>) : EntityClass<Snowflake, E>(table)
