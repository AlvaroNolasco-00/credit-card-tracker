package com.alvaronolasco.creditcardtracker.ui.expenses;

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
public final class ExpenseSearchViewModel_Factory implements Factory<ExpenseSearchViewModel> {
  private final Provider<CreditCardRepository> repositoryProvider;

  public ExpenseSearchViewModel_Factory(Provider<CreditCardRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ExpenseSearchViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ExpenseSearchViewModel_Factory create(
      Provider<CreditCardRepository> repositoryProvider) {
    return new ExpenseSearchViewModel_Factory(repositoryProvider);
  }

  public static ExpenseSearchViewModel newInstance(CreditCardRepository repository) {
    return new ExpenseSearchViewModel(repository);
  }
}
