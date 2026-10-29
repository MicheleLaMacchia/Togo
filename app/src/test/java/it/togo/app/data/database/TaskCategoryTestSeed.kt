package it.togo.app.data.database

import it.togo.app.data.database.entity.TaskCategoryEntity

/**
 * Seed stabile delle categorie Task, replicato nei test perché il database
 * in-memory non esegue la migrazione `5→6`.
 */
internal object TaskCategoryTestSeed {

    val categories: List<TaskCategoryEntity> = listOf(
        TaskCategoryEntity(1, null, "Casa", 1, 10),
        TaskCategoryEntity(2, null, "Commissioni", 1, 20),
        TaskCategoryEntity(3, null, "Personale", 1, 30),
        TaskCategoryEntity(4, null, "Lavoro", 1, 40),
        TaskCategoryEntity(5, null, "Altro", 1, 50),
        TaskCategoryEntity(101, 1, "Pulizie", 2, 10),
        TaskCategoryEntity(102, 1, "Manutenzione", 2, 20),
        TaskCategoryEntity(103, 1, "Organizzazione", 2, 30),
        TaskCategoryEntity(201, 2, "Ritiri", 2, 10),
        TaskCategoryEntity(202, 2, "Spedizioni", 2, 20),
        TaskCategoryEntity(203, 2, "Pratiche", 2, 30),
        TaskCategoryEntity(301, 3, "Appuntamenti", 2, 10),
        TaskCategoryEntity(302, 3, "Benessere", 2, 20),
        TaskCategoryEntity(303, 3, "Varie", 2, 30),
        TaskCategoryEntity(401, 4, "Attività", 2, 10),
        TaskCategoryEntity(402, 4, "Scadenze", 2, 20),
    )

    fun insert(db: TogoDatabase, rows: List<TaskCategoryEntity> = categories) {
        val writable = db.openHelper.writableDatabase
        rows.forEach { category ->
            writable.execSQL(
                """
                INSERT INTO TASK_CATEGORY (id, parentId, name, level, sortOrder)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    category.id,
                    category.parentId,
                    category.name,
                    category.level,
                    category.sortOrder,
                ),
            )
        }
    }
}
