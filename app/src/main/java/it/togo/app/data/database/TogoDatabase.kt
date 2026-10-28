package it.togo.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.dao.UserAttributeDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.data.database.entity.LearnedRuleEntity
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.data.database.entity.UserAttributeEntity

@Database(
    entities = [
        ShoppingItemEntity::class,
        HistoricalItemEntity::class,
        LearnedRuleEntity::class,
        TaxonomyLevel1Entity::class,
        TaxonomyLevel2Entity::class,
        TaxonomyLevel3Entity::class,
        CanonicalProductEntity::class,
        it.togo.app.data.database.entity.SynonymEntity::class,
        UserAttributeEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class TogoDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun historyDao(): HistoryDao
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
                        migration4To5(context.applicationContext)
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}