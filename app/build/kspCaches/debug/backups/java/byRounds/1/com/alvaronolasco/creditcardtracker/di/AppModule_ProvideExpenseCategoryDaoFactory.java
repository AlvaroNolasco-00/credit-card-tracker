package com.alvaronolasco.creditcardtracker.di;

import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseCategoryDao;
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
public final class AppModule_ProvideExpenseCategoryDaoFactory implements Factory<ExpenseCategoryDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideExpenseCategoryDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ExpenseCategoryDao get() {
    return provideExpenseCategoryDao(databaseProvider.get());
  }

  public static AppModule_ProvideExpenseCategoryDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideExpenseCategoryDaoFactory(databaseProvider);
  }

  public static ExpenseCategoryDao provideExpenseCategoryDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExpenseCategoryDao(database));
  }
}
