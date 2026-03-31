package com.alvaronolasco.creditcardtracker.di

import android.content.Context
import android.content.SharedPreferences
import com.alvaronolasco.creditcardtracker.data.AppDatabase
import com.alvaronolasco.creditcardtracker.data.dao.*
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): AppDatabase = AppDatabase.getDatabase(context, scope)

    @Provides
    fun provideCreditCardDao(database: AppDatabase): CreditCardDao = database.creditCardDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideExpenseCategoryDao(database: AppDatabase): ExpenseCategoryDao = database.expenseCategoryDao()

    @Provides
    fun provideNotificationConfigDao(database: AppDatabase): NotificationConfigDao = database.notificationConfigDao()

    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao = database.incomeDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideRepository(
        cardDao: CreditCardDao,
        categoryDao: CategoryDao,
        expenseDao: ExpenseDao,
        expenseCategoryDao: ExpenseCategoryDao,
        configDao: NotificationConfigDao,
        incomeDao: IncomeDao,
        budgetDao: BudgetDao
    ): CreditCardRepository = CreditCardRepository(cardDao, categoryDao, expenseDao, expenseCategoryDao, configDao, incomeDao, budgetDao)
}
