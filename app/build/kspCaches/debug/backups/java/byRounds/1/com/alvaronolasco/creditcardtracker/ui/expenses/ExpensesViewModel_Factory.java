package com.alvaronolasco.creditcardtracker.ui.expenses;

import android.content.Context;
import com.alvaronolasco.creditcardtracker.data.repository.CreditCardRepository;
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
public final class ExpensesViewModel_Factory implements Factory<ExpensesViewModel> {
  private final Provider<CreditCardRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public ExpensesViewModel_Factory(Provider<CreditCardRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ExpensesViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static ExpensesViewModel_Factory create(Provider<CreditCardRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new ExpensesViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static ExpensesViewModel newInstance(CreditCardRepository repository, Context context) {
    return new ExpensesViewModel(repository, context);
  }
}
