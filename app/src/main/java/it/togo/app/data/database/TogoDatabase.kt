package it.togo.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.data.database.entity.LearnedRuleEntity
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.data.database.entity.TypeConverters

@Database(
    entities = [
        ShoppingItemEntity::class,
        HistoricalItemEntity::class,
        LearnedRuleEntity::class,
        TaxonomyLevel1Entity::class,
        TaxonomyLevel2Entity::class,
        TaxonomyLevel3Entity::class,
        CanonicalProductEntity::class,
        it.togo.app.data.database.entity.SynonymEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TypeConverters::class)
abstract class TogoDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun learnedRulesDao(): LearnedRulesDao
    abstract fun catalogDao(): CatalogDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Catalog seed will be handled in Story 1.4 via createFromAsset()
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}