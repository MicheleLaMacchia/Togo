package it.togo.app.data.database.entity

import androidx.room.TypeConverter
import it.togo.app.domain.model.StandardUnit
import java.util.UUID

class TypeConverters {

    @TypeConverter
    fun standardUnitToString(unit: StandardUnit?): String? = unit?.code

    @TypeConverter
    fun stringToStandardUnit(code: String?): StandardUnit? = code?.let { StandardUnit.fromCode(it) }

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun stringToUuid(str: String?): UUID? = str?.let { UUID.fromString(it) }

    @TypeConverter
    fun uuidStringToString(uuidStr: String?): String? = uuidStr

    @TypeConverter
    fun stringToUuidString(str: String?): String? = str
}