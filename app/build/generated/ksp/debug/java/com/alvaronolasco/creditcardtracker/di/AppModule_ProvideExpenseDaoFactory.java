package com.alvaronolasco.creditcardtracker.di;

import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideExpenseDaoFactory implements Factory<ExpenseDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideExpenseDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ExpenseDao get() {
    return provideExpenseDao(databaseProvider.get());
  }

  public static AppModule_ProvideExpenseDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideExpenseDaoFactory(databaseProvider);
  }

  public static ExpenseDao provideExpenseDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExpenseDao(database));
  }
}
