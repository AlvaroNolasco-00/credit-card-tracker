package com.alvaronolasco.creditcardtracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CreditCard::class,
        Category::class,
        Expense::class,
        ExpenseCategory::class,
        NotificationConfig::class,
        IncomeProfile::class,
        IncomeEntry::class,
        BudgetItem::class,
        ActivityLog::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun notificationConfigDao(): NotificationConfigDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE credit_cards ADD COLUMN lastPaymentDate INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE credit_cards ADD COLUMN partialPaymentAmount REAL NOT NULL DEFAULT 0.0"
                )
                database.execSQL(
                    "ALTER TABLE credit_cards ADD COLUMN partialPaymentCycleEnd INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        action TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        entityId INTEGER,
                        entityType TEXT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_activity_logs_category ON activity_logs(category)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_activity_logs_timestamp ON activity_logs(timestamp)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budget_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        monthYear TEXT NOT NULL,
                        limitAmount REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_budget_items_categoryId ON budget_items(categoryId)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_budget_items_categoryId_monthYear ON budget_items(categoryId, monthYear)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: AppDatabase? = null
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "credit_card_tracker_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            instance?.categoryDao()?.let { dao ->
                                seedDefaultCategories(dao)
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        scope.launch(Dispatchers.IO) {
                            instance?.categoryDao()?.let { dao ->
                                seedDefaultCategories(dao)
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
            val existing = categoryDao.getDefaultCategories()
            if (existing.isEmpty()) {
                val defaultCategories = listOf(
                    Category(name = "Entretenimiento", icon = "entertainment", isDefault = true),
                    Category(name = "Transporte", icon = "transport", isDefault = true),
                    Category(name = "Comida", icon = "food", isDefault = true),
                    Category(name = "Medicina", icon = "medicine", isDefault = true)
                )
                defaultCategories.forEach { categoryDao.insertCategory(it) }
            }
        }
    }
}
