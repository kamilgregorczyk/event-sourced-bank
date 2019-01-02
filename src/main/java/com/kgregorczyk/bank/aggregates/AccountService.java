package com.kgregorczyk.bank.aggregates;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import java.math.BigDecimal;
import java.util.UUID;

public class AccountService {

  private final EventBus eventBus;

  public AccountService(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  /**
   * Command which initializes  {@link AccountAggregate} by emitting {@link AccountCreatedEvent}
   * with only {@code fullName}. This event is then received {@link EventManager}.
   */
  public void asyncCreateAccountCommand(String fullName) {
    eventBus.post(new AccountCreatedEvent(UUID.randomUUID(), fullName));
  }

  /**
   * Command which updates aggregate's {@code fullName} field by emitting {@link
   * FullNameChangedEvent}. This event is then received {@link EventManager}.
   */
  public void asyncChangeFullNameCommand(UUID uuid, String fullName) {
    eventBus.post(new FullNameChangedEvent(uuid, fullName));
  }

  /**
   * Command which initializes money transfer between two aggregates by emitting {@link
   * MoneyTransferredEvent}. This event is then received {@link EventManager}.
   */
  public void asyncTransferMoneyCommand(UUID fromUUID, UUID toUUID, BigDecimal value) {
    eventBus.post(new MoneyTransferredEvent(fromUUID, fromUUID, toUUID, UUID.randomUUID(), value));
  }
}
