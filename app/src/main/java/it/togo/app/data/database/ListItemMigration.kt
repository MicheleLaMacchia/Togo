package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrazione 5→6: crea le tabelle della lista unificata e delle Task e copia
 * i dati legacy.
 *
 * Questa è la variante concordata per la Story 8.4: `SHOPPING_ITEM` e
 * `HISTORICAL_ITEM` **non** vengono eliminate, perché il codice legacy è
 * ancora registrato in [TogoDatabase]. La loro rimozione avverrà nella Story
 * 8.7 con la migrazione 6→7.
 *
 * Room esegue `migrate` in una transazione; la transazione esplicita qui sotto
 * rende atomico anche l'harness che invoca direttamente la migrazione.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            createListItem(db)
            copyShoppingItems(db)
            copyHistoricalItems(db)
            createTaskCategory(db)
            seedTaskCategories(db)
            createTodoTask(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

private fun createListItem(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `LIST_ITEM` (
            `id` TEXT NOT NULL,
            `productId` INTEGER NOT NULL,
            `quantity` REAL NOT NULL,
            `unit` TEXT NOT NULL,
            `brand` TEXT,
            `variant` TEXT,
            `condition` TEXT,
            `note` TEXT,
            `status` TEXT NOT NULL,
            `purchaseCount` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `statusChangedAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_list_item_product_id` " +
            "ON `LIST_ITEM` (`productId`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_list_item_status` " +
            "ON `LIST_ITEM` (`status`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_list_item_product_status` " +
            "ON `LIST_ITEM` (`productId`, `status`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_list_item_created_at` " +
            "ON `LIST_ITEM` (`createdAt`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_list_item_status_changed_at` " +
            "ON `LIST_ITEM` (`statusChangedAt`)",
    )
}

private fun copyShoppingItems(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO LIST_ITEM (
            id, productId, quantity, unit, brand, variant, condition, note,
            status, purchaseCount, createdAt, statusChangedAt, updatedAt
        )
        SELECT
            id,
            productId,
            CASE
                WHEN quantity IS NOT NULL AND quantity > 0 AND quantity < 1e308
                THEN quantity
                ELSE 1.0
            END,
            CASE
                WHEN unit IS NULL OR TRIM(unit) = '' THEN 'PIECE'
                ELSE unit
            END,
            brand,
            variant,
            condition,
            NULL,
            CASE WHEN isChecked = 1 THEN 'PURCHASED' ELSE 'TO_BUY' END,
            CASE WHEN isChecked = 1 THEN 1 ELSE 0 END,
            createdAt,
            CASE WHEN isChecked = 1 THEN updatedAt ELSE createdAt END,
            updatedAt
        FROM SHOPPING_ITEM
        """.trimIndent(),
    )
}

private fun copyHistoricalItems(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO LIST_ITEM (
            id, productId, quantity, unit, brand, variant, condition, note,
            status, purchaseCount, createdAt, statusChangedAt, updatedAt
        )
        SELECT
            'legacy-hist:' || id,
            productId,
            CASE
                WHEN lastQuantity IS NOT NULL AND lastQuantity > 0 AND lastQuantity < 1e308
                THEN lastQuantity
                ELSE 1.0
            END,
            CASE
                WHEN lastUnit IS NULL OR TRIM(lastUnit) = '' THEN 'PIECE'
                ELSE lastUnit
            END,
            brand,
            variant,
            condition,
            NULL,
            'PURCHASED',
            CASE WHEN purchaseCount < 1 THEN 1 ELSE purchaseCount END,
            purchasedAt,
            purchasedAt,
            purchasedAt
        FROM HISTORICAL_ITEM
        """.trimIndent(),
    )
}

private fun createTaskCategory(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `TASK_CATEGORY` (
            `id` INTEGER NOT NULL,
            `parentId` INTEGER,
            `name` TEXT NOT NULL,
            `level` INTEGER NOT NULL,
            `sortOrder` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`parentId`) REFERENCES `TASK_CATEGORY`(`id`)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_task_category_parent` " +
            "ON `TASK_CATEGORY` (`parentId`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_task_category_sort` " +
            "ON `TASK_CATEGORY` (`sortOrder`)",
    )
}

private fun seedTaskCategories(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT OR IGNORE INTO TASK_CATEGORY (id, parentId, name, level, sortOrder) VALUES
        (1, NULL, 'Casa', 1, 10),
        (2, NULL, 'Commissioni', 1, 20),
        (3, NULL, 'Personale', 1, 30),
        (4, NULL, 'Lavoro', 1, 40),
        (5, NULL, 'Altro', 1, 50),
        (101, 1, 'Pulizie', 2, 10),
        (102, 1, 'Manutenzione', 2, 20),
        (103, 1, 'Organizzazione', 2, 30),
        (201, 2, 'Ritiri', 2, 10),
        (202, 2, 'Spedizioni', 2, 20),
        (203, 2, 'Pratiche', 2, 30),
        (301, 3, 'Appuntamenti', 2, 10),
        (302, 3, 'Benessere', 2, 20),
        (303, 3, 'Varie', 2, 30),
        (401, 4, 'Attività', 2, 10),
        (402, 4, 'Scadenze', 2, 20)
        """.trimIndent(),
    )
}

private fun createTodoTask(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `TODO_TASK` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `normalizedTitle` TEXT NOT NULL,
            `categoryId` INTEGER,
            `priority` TEXT NOT NULL,
            `dueDate` TEXT,
            `note` TEXT,
            `status` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `statusChangedAt` INTEGER NOT NULL,
            `completedAt` INTEGER,
            FOREIGN KEY(`categoryId`) REFERENCES `TASK_CATEGORY`(`id`)
                ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_todo_task_status` " +
            "ON `TODO_TASK` (`status`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_todo_task_category` " +
            "ON `TODO_TASK` (`categoryId`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_todo_task_created_at` " +
            "ON `TODO_TASK` (`createdAt`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_todo_task_status_priority_created` " +
            "ON `TODO_TASK` (`status`, `priority`, `createdAt`)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `idx_todo_task_due_date` " +
            "ON `TODO_TASK` (`dueDate`)",
    )
}
