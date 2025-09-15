package ai.arcblroth.somnus3.data

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.core.vendors.currentDialect

class SnowflakeColumnType : ColumnType<Snowflake>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.ulongType()

    override fun valueFromDB(value: Any): Snowflake =
        when (value) {
            is ULong -> Snowflake(value)
            is Long -> Snowflake(value)
            is Number -> Snowflake(value.toLong())
            is String -> Snowflake(value)
            else -> error("Unexpected value of type Snowflake: $value of ${value::class.qualifiedName}")
        }

    override fun setParameter(
        stmt: PreparedStatementApi,
        index: Int,
        value: Any?,
    ) {
        val v = if (value is Snowflake) value.value.toLong() else value
        super.setParameter(stmt, index, v)
    }

    override fun notNullValueToDB(value: Snowflake) = value.value.toLong()
}

fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType())

open class SnowflakeIdTable(
    name: String = "",
    columnName: String = "id",
) : IdTable<Snowflake>(name) {
    final override val id: Column<EntityID<Snowflake>> = snowflake(columnName).entityId()
    final override val primaryKey = PrimaryKey(id)
}
