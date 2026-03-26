package com.alvaronolasco.creditcardtracker.di;

import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import com.alvaronolasco.creditcardtracker.data.dao.IncomeDao;
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
public final class AppModule_ProvideIncomeDaoFactory implements Factory<IncomeDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideIncomeDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public IncomeDao get() {
    return provideIncomeDao(databaseProvider.get());
  }

  public static AppModule_ProvideIncomeDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideIncomeDaoFactory(databaseProvider);
  }

  public static IncomeDao provideIncomeDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideIncomeDao(database));
  }
}
