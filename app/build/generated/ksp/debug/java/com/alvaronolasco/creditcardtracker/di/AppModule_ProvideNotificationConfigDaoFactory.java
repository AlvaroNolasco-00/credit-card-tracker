package com.alvaronolasco.creditcardtracker.di;

import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import com.alvaronolasco.creditcardtracker.data.dao.NotificationConfigDao;
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
public final class AppModule_ProvideNotificationConfigDaoFactory implements Factory<NotificationConfigDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideNotificationConfigDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public NotificationConfigDao get() {
    return provideNotificationConfigDao(databaseProvider.get());
  }

  public static AppModule_ProvideNotificationConfigDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideNotificationConfigDaoFactory(databaseProvider);
  }

  public static NotificationConfigDao provideNotificationConfigDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNotificationConfigDao(database));
  }
}
