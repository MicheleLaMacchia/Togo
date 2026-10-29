package it.togo.app.domain.model

import it.togo.app.data.database.entity.TaskCategoryEntity

/**
 * Categoria Task a due livelli.
 *
 * Il livello 1 rappresenta un'Area; il livello 2 una sottocategoria e deve
 * avere come parent un'Area. Il livello 3 non è ammesso in v1.
 */
data class TaskCategory(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val level: Int,
    val sortOrder: Int,
) {

    init {
        require(id > 0) { "L'id della categoria Task deve essere maggiore di zero" }
        require(name.isNotBlank()) { "Il nome della categoria Task non può essere vuoto" }
        require(level == 1 || level == 2) {
            "Il livello della categoria Task deve essere 1 o 2, ricevuto: $level"
        }
        require(level != 1 || parentId == null) {
            "Una categoria Task di livello 1 non può avere un parent"
        }
        require(level != 2 || parentId != null) {
            "Una categoria Task di livello 2 deve avere un parent"
        }
    }

    fun toEntity(): TaskCategoryEntity = TaskCategoryEntity(
        id = id,
        parentId = parentId,
        name = name,
        level = level,
        sortOrder = sortOrder,
    )
}

fun TaskCategoryEntity.toDomain(): TaskCategory = TaskCategory(
    id = id,
    parentId = parentId,
    name = name,
    level = level,
    sortOrder = sortOrder,
)
