package com.alvaronolasco.creditcardtracker.di;

import android.content.Context;
import com.alvaronolasco.creditcardtracker.data.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideDatabaseFactory implements Factory<AppDatabase> {
  private final Provider<Context> contextProvider;

  private final Provider<CoroutineScope> scopeProvider;

  public AppModule_ProvideDatabaseFactory(Provider<Context> contextProvider,
      Provider<CoroutineScope> scopeProvider) {
    this.contextProvider = contextProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public AppDatabase get() {
    return provideDatabase(contextProvider.get(), scopeProvider.get());
  }

  public static AppModule_ProvideDatabaseFactory create(Provider<Context> contextProvider,
      Provider<CoroutineScope> scopeProvider) {
    return new AppModule_ProvideDatabaseFactory(contextProvider, scopeProvider);
  }

  public static AppDatabase provideDatabase(Context context, CoroutineScope scope) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDatabase(context, scope));
  }
}
