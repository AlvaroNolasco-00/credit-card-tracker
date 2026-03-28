package com.alvaronolasco.creditcardtracker.data

import android.content.Context
import androidx.room.*
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
        IncomeEntry::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun notificationConfigDao(): NotificationConfigDao
    abstract fun incomeDao(): IncomeDao

    companion object {
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
