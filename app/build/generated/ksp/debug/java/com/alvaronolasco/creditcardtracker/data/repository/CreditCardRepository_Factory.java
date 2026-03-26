package com.alvaronolasco.creditcardtracker.data.repository;

import com.alvaronolasco.creditcardtracker.data.dao.CategoryDao;
import com.alvaronolasco.creditcardtracker.data.dao.CreditCardDao;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseCategoryDao;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseDao;
import com.alvaronolasco.creditcardtracker.data.dao.IncomeDao;
import com.alvaronolasco.creditcardtracker.data.dao.NotificationConfigDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class CreditCardRepository_Factory implements Factory<CreditCardRepository> {
  private final Provider<CreditCardDao> cardDaoProvider;

  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<ExpenseDao> expenseDaoProvider;

  private final Provider<ExpenseCategoryDao> expenseCategoryDaoProvider;

  private final Provider<NotificationConfigDao> configDaoProvider;

  private final Provider<IncomeDao> incomeDaoProvider;

  public CreditCardRepository_Factory(Provider<CreditCardDao> cardDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<ExpenseCategoryDao> expenseCategoryDaoProvider,
      Provider<NotificationConfigDao> configDaoProvider, Provider<IncomeDao> incomeDaoProvider) {
    this.cardDaoProvider = cardDaoProvider;
    this.categoryDaoProvider = categoryDaoProvider;
    this.expenseDaoProvider = expenseDaoProvider;
    this.expenseCategoryDaoProvider = expenseCategoryDaoProvider;
    this.configDaoProvider = configDaoProvider;
    this.incomeDaoProvider = incomeDaoProvider;
  }

  @Override
  public CreditCardRepository get() {
    return newInstance(cardDaoProvider.get(), categoryDaoProvider.get(), expenseDaoProvider.get(), expenseCategoryDaoProvider.get(), configDaoProvider.get(), incomeDaoProvider.get());
  }

  public static CreditCardRepository_Factory create(Provider<CreditCardDao> cardDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<ExpenseCategoryDao> expenseCategoryDaoProvider,
      Provider<NotificationConfigDao> configDaoProvider, Provider<IncomeDao> incomeDaoProvider) {
    return new CreditCardRepository_Factory(cardDaoProvider, categoryDaoProvider, expenseDaoProvider, expenseCategoryDaoProvider, configDaoProvider, incomeDaoProvider);
  }

  public static CreditCardRepository newInstance(CreditCardDao cardDao, CategoryDao categoryDao,
      ExpenseDao expenseDao, ExpenseCategoryDao expenseCategoryDao, NotificationConfigDao configDao,
      IncomeDao incomeDao) {
    return new CreditCardRepository(cardDao, categoryDao, expenseDao, expenseCategoryDao, configDao, incomeDao);
  }
}
