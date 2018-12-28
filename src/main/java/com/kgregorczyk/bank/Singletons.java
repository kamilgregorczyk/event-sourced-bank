package com.kgregorczyk.bank;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.AccountEventProcessor;
import com.kgregorczyk.bank.aggregates.AccountEventStorage;

/**
 * Contains static references to shared dependencies.
 *
 * TODO(gregorczyk): Replace it with proper DI mechanism like Guice.
 */
public class Singletons {

  public static final EventBus EVENT_BUS = new EventBus("account-bus");
  public static final AccountEventProcessor EVENT_MANAGER = new AccountEventProcessor();
  public static final AccountEventStorage ACCOUNT_EVENT_STORAGE = new AccountEventStorage();

}
