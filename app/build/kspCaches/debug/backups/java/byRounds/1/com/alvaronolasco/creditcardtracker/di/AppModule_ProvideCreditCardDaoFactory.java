package com.alvaronolasco.creditcardtracker.di;

import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import com.alvaronolasco.creditcardtracker.data.dao.CreditCardDao;
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
public final class AppModule_ProvideCreditCardDaoFactory implements Factory<CreditCardDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideCreditCardDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CreditCardDao get() {
    return provideCreditCardDao(databaseProvider.get());
  }

  public static AppModule_ProvideCreditCardDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideCreditCardDaoFactory(databaseProvider);
  }

  public static CreditCardDao provideCreditCardDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCreditCardDao(database));
  }
}
