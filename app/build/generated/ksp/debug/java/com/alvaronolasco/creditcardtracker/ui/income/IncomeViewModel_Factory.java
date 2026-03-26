package com.alvaronolasco.creditcardtracker.ui.income;

import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository;
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
public final class IncomeViewModel_Factory implements Factory<IncomeViewModel> {
  private final Provider<CreditCardRepository> repositoryProvider;

  public IncomeViewModel_Factory(Provider<CreditCardRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public IncomeViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static IncomeViewModel_Factory create(Provider<CreditCardRepository> repositoryProvider) {
    return new IncomeViewModel_Factory(repositoryProvider);
  }

  public static IncomeViewModel newInstance(CreditCardRepository repository) {
    return new IncomeViewModel(repository);
  }
}
