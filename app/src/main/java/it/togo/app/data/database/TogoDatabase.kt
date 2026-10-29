package it.togo.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.dao.ListItemDao
import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.dao.TaskCategoryDao
import it.togo.app.data.database.dao.TodoTaskDao
import it.togo.app.data.database.dao.UserAttributeDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.data.database.entity.LearnedRuleEntity
import it.togo.app.data.database.entity.ListItemEntity
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.data.database.entity.TaskCategoryEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.data.database.entity.TogoTypeConverters
import it.togo.app.data.database.entity.TodoTaskEntity
import it.togo.app.data.database.entity.UserAttributeEntity

@TypeConverters(TogoTypeConverters::class)
@Database(
    entities = [
        // Legacy temporaneo: rimosso nella Story 8.7 con la migrazione 6→7.
        ShoppingItemEntity::class,
        HistoricalItemEntity::class,
        // Nuove tabelle della lista unificata e delle Task (Story 8.4).
        ListItemEntity::class,
        TaskCategoryEntity::class,
        TodoTaskEntity::class,
        LearnedRuleEntity::class,
        TaxonomyLevel1Entity::class,
        TaxonomyLevel2Entity::class,
        TaxonomyLevel3Entity::class,
        CanonicalProductEntity::class,
        it.togo.app.data.database.entity.SynonymEntity::class,
        UserAttributeEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class TogoDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun listItemDao(): ListItemDao
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun taskCategoryDao(): TaskCategoryDao
    abstract fun learnedRulesDao(): LearnedRulesDao
    abstract fun catalogDao(): CatalogDao
    abstract fun userAttributeDao(): UserAttributeDao

    companion object {
        @Volatile
        private var INSTANCE: TogoDatabase? = null

        fun build(context: Context): TogoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TogoDatabase::class.java,
                    "togo.db"
                )
                    // Story 1.4: catalogo precompilato copiato dagli asset al primo avvio (AD-3).
                    // La migrazione 4→5 applica il mapping
                    // `tools/togo_catalog_migration_mapping_v2.csv` ai DB già
                    // esistenti: rimappa i riferimenti utente, sostituisce i soli
                    // prodotti predefiniti e lascia intatti quelli creati dall'utente.
                    // Un DB dev senza catalogo (es. Story 1.3) viene popolato dalla
                    // stessa migrazione perché il conteggio canonico non è 748.
                    //
                    // Story 7.1 (2026-10-22): `CanonicalProductEntity.id` usa
                    // `autoGenerate`, quindi l'identity hash dello schema Room cambia
                    // pur restando version = 2. Un "togo.db" creato da una build
                    // precedente fallisce il check di identità all'avvio.
                    // Story 7.2 (2026-10-22): anche `TaxonomyLevel3Entity.id` usa
                    // `autoGenerate`; l'identity hash cambia di nuovo.
                    // Opzione accettata per quei DB pre-v4: azzerare i dati una volta
                    // (disinstallare l'app o `adb shell pm clear it.togo.app`).
                    .createFromAsset("catalog.db")
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        migration4To5(context.applicationContext),
                        MIGRATION_5_6
                    )
                    // Nessun fallback distruttivo: un errore di migrazione deve
                    // lasciare il database alla versione precedente.
                    //
                    // Story 8.4: SHOPPING_ITEM e HISTORICAL_ITEM restano
                    // temporaneamente in vita insieme a LIST_ITEM perché il
                    // codice legacy è ancora registrato. La loro rimozione e la
                    // migrazione 6→7 sono pianificate nella Story 8.7.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}