package com.alvaronolasco.creditcardtracker.notifications;

import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<CreditCardRepository> repositoryProvider;

  private final Provider<ReminderScheduler> schedulerProvider;

  public BootReceiver_MembersInjector(Provider<CreditCardRepository> repositoryProvider,
      Provider<ReminderScheduler> schedulerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.schedulerProvider = schedulerProvider;
  }

  public static MembersInjector<BootReceiver> create(
      Provider<CreditCardRepository> repositoryProvider,
      Provider<ReminderScheduler> schedulerProvider) {
    return new BootReceiver_MembersInjector(repositoryProvider, schedulerProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectRepository(instance, repositoryProvider.get());
    injectScheduler(instance, schedulerProvider.get());
  }

  @InjectedFieldSignature("com.alvaronolasco.creditcardtracker.notifications.BootReceiver.repository")
  public static void injectRepository(BootReceiver instance, CreditCardRepository repository) {
    instance.repository = repository;
  }

  @InjectedFieldSignature("com.alvaronolasco.creditcardtracker.notifications.BootReceiver.scheduler")
  public static void injectScheduler(BootReceiver instance, ReminderScheduler scheduler) {
    instance.scheduler = scheduler;
  }
}
