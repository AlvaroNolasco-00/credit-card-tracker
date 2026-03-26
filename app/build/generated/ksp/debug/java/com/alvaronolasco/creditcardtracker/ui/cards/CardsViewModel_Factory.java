package com.alvaronolasco.creditcardtracker.ui.cards;

import android.content.Context;
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository;
import com.alvaronolasco.creditcardtracker.notifications.ReminderScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class CardsViewModel_Factory implements Factory<CardsViewModel> {
  private final Provider<CreditCardRepository> repositoryProvider;

  private final Provider<ReminderScheduler> schedulerProvider;

  private final Provider<Context> contextProvider;

  public CardsViewModel_Factory(Provider<CreditCardRepository> repositoryProvider,
      Provider<ReminderScheduler> schedulerProvider, Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.schedulerProvider = schedulerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public CardsViewModel get() {
    return newInstance(repositoryProvider.get(), schedulerProvider.get(), contextProvider.get());
  }

  public static CardsViewModel_Factory create(Provider<CreditCardRepository> repositoryProvider,
      Provider<ReminderScheduler> schedulerProvider, Provider<Context> contextProvider) {
    return new CardsViewModel_Factory(repositoryProvider, schedulerProvider, contextProvider);
  }

  public static CardsViewModel newInstance(CreditCardRepository repository,
      ReminderScheduler scheduler, Context context) {
    return new CardsViewModel(repository, scheduler, context);
  }
}
