package it.togo.app.data.database.entity

import androidx.room.TypeConverter
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Converter delle nuove entità Task.
 *
 * Non sostituisce [TypeConverters]: quella classe resta disponibile per le
 * entità legacy. `ListItemEntity.status` continua a usare una stringa, quindi
 * non richiede un converter dedicato.
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
