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
        ActivityLog::class,
        RecurringExpense::class,
        RecurringExpenseCategory::class,
        SyncQueueItem::class
    ],
    version = 16,
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
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE credit_cards ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE budget_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE income_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE income_profiles ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recurring_expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notification_configs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        lastAttempt INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_queue_entity ON sync_queue(entityType, entityId)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        dayOfMonth INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_expense_categories (
                        recurringExpenseId INTEGER NOT NULL,
                        categoryId INTEGER NOT NULL,
                        PRIMARY KEY(recurringExpenseId, categoryId),
                        FOREIGN KEY(recurringExpenseId) REFERENCES recurring_expenses(id) ON DELETE CASCADE,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_expenses_cardId ON recurring_expenses(cardId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_rec_categories_expenseId ON recurring_expense_categories(recurringExpenseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_rec_categories_categoryId ON recurring_expense_categories(categoryId)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        dayOfMonth INTEGER,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO recurring_expenses_new (id, cardId, amount, description, dayOfMonth, isActive, createdAt)
                    SELECT id, cardId, amount, description, dayOfMonth, isActive, createdAt
                    FROM recurring_expenses
                """.trimIndent())
                database.execSQL("DROP TABLE recurring_expenses")
                database.execSQL("ALTER TABLE recurring_expenses_new RENAME TO recurring_expenses")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expenses_cardId ON recurring_expenses(cardId)")

                database.execSQL("DROP INDEX IF EXISTS idx_rec_categories_expenseId")
                database.execSQL("DROP INDEX IF EXISTS idx_rec_categories_categoryId")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expense_categories_recurringExpenseId ON recurring_expense_categories(recurringExpenseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expense_categories_categoryId ON recurring_expense_categories(categoryId)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE activity_logs ADD COLUMN amount REAL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE credit_cards ADD COLUMN bankId TEXT")
            }
        }

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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        receiptImagePath TEXT,
                        ocrRawText TEXT,
                        date INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        msiMonths INTEGER NOT NULL DEFAULT 1,
                        msiMonthlyAmount REAL NOT NULL DEFAULT 0.0,
                        msiEndDate INTEGER NOT NULL DEFAULT 0,
                        paymentMethod TEXT NOT NULL DEFAULT 'CREDIT_CARD',
                        FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO expenses_new (id, cardId, amount, description, receiptImagePath, ocrRawText, date, createdAt, msiMonths, msiMonthlyAmount, msiEndDate, paymentMethod)
                    SELECT id, cardId, amount, description, receiptImagePath, ocrRawText, date, createdAt, msiMonths, msiMonthlyAmount, msiEndDate, 'CREDIT_CARD'
                    FROM expenses
                """.trimIndent())
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_cardId ON expenses(cardId)")
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
                .addMigrations(
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
                    MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                    MIGRATION_15_16
                )
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
