package com.kgregorczyk.bank.aggregates;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import java.math.BigDecimal;
import java.util.UUID;

public class AccountService {

  private final EventBus eventBus;

  public AccountService(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  /**
   * Command which initializes {@link AccountAggregate} by emitting {@link AccountCreatedEvent} with
   * only {@code fullName}. This event is then received {@link EventManager}.
   */
  public UUID asyncCreateAccountCommand(String fullName) {
    UUID aggregateUUID = UUID.randomUUID();
    eventBus.post(new AccountCreatedEvent(aggregateUUID, fullName));
    return aggregateUUID;
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

  /**
   * Command which cancels money transfer on a single aggregate by emitting {@link
   * MoneyTransferCancelled}. This event is then received {@link EventManager}.
   */
  public void asyncCancelTransactionCommand(
      UUID aggregateUUID,
      UUID fromUUID,
      UUID toUUID,
      UUID transactionUUID,
      BigDecimal value,
      Reason reason) {
    eventBus.post(
        new MoneyTransferCancelled(
            aggregateUUID, fromUUID, toUUID, transactionUUID, value, reason));
  }
}
