package it.togo.app.data.database.entity

import androidx.room.TypeConverter
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Converter delle entità Task.
 *
 * Dalla Story 8.7 è l'unica classe di converter registrata: la vecchia
 * `TypeConverters` serviva solo alle entità legacy ed è stata rimossa.
 * `ListItemEntity.status` resta una stringa e non richiede un converter.
 */
class TogoTypeConverters {

    @TypeConverter
    fun todoPriorityToString(value: TodoPriority): String = value.name

    @TypeConverter
    fun stringToTodoPriority(value: String): TodoPriority = TodoPriority.valueOf(value)

    @TypeConverter
    fun todoStatusToString(value: TodoStatus): String = value.name

    @TypeConverter
    fun stringToTodoStatus(value: String): TodoStatus = TodoStatus.valueOf(value)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? =
        value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
}
