package com.kgregorczyk.bank.aggregates;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountCreditedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountDebitedEvent;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferSucceeded;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener and dispatcher of events in whole system. It contains dependencies between events.
 */
@Slf4j
public class EventManager {

  private final EventBus eventBus;
  private final AccountEventStorage eventStorage;

  public EventManager(EventBus eventBus, AccountEventStorage eventStorage) {
    this.eventBus = eventBus;
    this.eventStorage = eventStorage;
  }

  // Event Handlers
  @Subscribe
  void handle(AccountCreatedEvent event) {
    logEvent(event);
    eventStorage.save(event);
  }

  @Subscribe
  void handle(FullNameChangedEvent event) {
    logEvent(event);
    persistIfAggregateExists(event);
  }

  @Subscribe
  void handle(MoneyTransferredEvent event) {
    logEvent(event);
    persistIfAggregateExists(event);
    eventBus.post(
        new AccountDebitedEvent(event.getAggregateUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  void handle(AccountDebitedEvent event) {
    logEvent(event);
    AccountAggregate dirtyAggregate = eventStorage.loadByUUID(event.getFromUUID());
    try {
      dirtyAggregate.apply(event);
    } catch (BalanceTooLowException e) {
      // When there's not enough money MoneyTransferCancelled should be emitted only to issuer
      eventBus.post(
          new MoneyTransferCancelled(event.getAggregateUUID(), event.getFromUUID(),
              event.getToUUID(),
              event.getTransactionUUID(), event.getValue(), Reason.BALANCE_TOO_LOW));
      return;
    }
    // When there's enough money we persist event and progress further
    persistIfAggregateExists(event);
    // Saves MoneyTransferredEvent in receiver's aggregate
    persistIfAggregateExists(
        new MoneyTransferredEvent(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
    // Requests crediting receiver's aggregate
    eventBus.post(
        new AccountCreditedEvent(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  void handle(AccountCreditedEvent event) {
    logEvent(event);
    persistIfAggregateExists(event);

    // Marks transfer as succeeded in issuer account
    eventBus.post(
        new MoneyTransferSucceeded(event.getFromUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));

    // Marks transfer as succeeded in receiver account
    eventBus.post(
        new MoneyTransferSucceeded(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  void handle(MoneyTransferSucceeded event) {
    logEvent(event);
    persistIfAggregateExists(event);
  }


  @Subscribe
  void handle(MoneyTransferCancelled event) {
    logEvent(event);
    persistIfAggregateExists(event);
  }

  private void logEvent(DomainEvent event) {
    log.info("Received event: {}", event);
  }

  private void persistIfAggregateExists(DomainEvent event) {
    if (eventStorage.exists(event.getAggregateUUID())) {
      log.debug("Persisted {} event: {}", event.getEventType(), event);
      eventStorage.save(event);
    } else {
      throw new AggregateDoesNotExist(event.toString());
    }
  }
}
