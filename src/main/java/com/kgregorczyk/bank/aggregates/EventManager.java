package com.kgregorczyk.bank.aggregates;

import static com.kgregorczyk.bank.Singletons.ACCOUNT_EVENT_STORAGE;
import static com.kgregorczyk.bank.Singletons.EVENT_BUS;

import com.google.common.eventbus.Subscribe;
import com.kgregorczyk.bank.aggregates.events.AccountDebitedEvent;
import com.kgregorczyk.bank.aggregates.events.CancelMoneyTransfer;
import com.kgregorczyk.bank.aggregates.events.ChangeFullNameEvent;
import com.kgregorczyk.bank.aggregates.events.CreateAccountEvent;
import com.kgregorczyk.bank.aggregates.events.CreditAccountEvent;
import com.kgregorczyk.bank.aggregates.events.DebitAccountEvent;
import com.kgregorczyk.bank.aggregates.events.SuccessMoneyTransfer;
import com.kgregorczyk.bank.aggregates.events.TransferMoneyEvent;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener and dispatcher of events in whole system. It contains dependencies between events.
 */
@Slf4j
public class EventManager {

  // Event Handlers
  @Subscribe
  static void handle(CreateAccountEvent event) {
    log.info("Received CreateAccountEvent: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);
  }

  @Subscribe
  static void handle(ChangeFullNameEvent event) {
    log.info("Received ChangeFullNameEvent: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);
  }

  @Subscribe
  static void handle(TransferMoneyEvent event) {
    log.info("Received TransferMoneyEvent: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);
    EVENT_BUS.post(
        new DebitAccountEvent(event.getAggregateUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  static void handle(DebitAccountEvent event) {
    log.info("Received DebitAccountEvent: {}", event);
    AccountAggregate dirtyAggregate = ACCOUNT_EVENT_STORAGE.loadByUUID(event.getFromUUID())
        .apply(event);

    if (dirtyAggregate.getBalance().compareTo(BigDecimal.ZERO) < 0) {
      // When there's not enough money CancelMoneyTransfer should be emitted only to issuer
      EVENT_BUS.post(
          new CancelMoneyTransfer(event.getAggregateUUID(), event.getFromUUID(), event.getToUUID(),
              event.getTransactionUUID(), event.getValue()));
    } else {
      // When there's enough money we persist event and send signal about success
      ACCOUNT_EVENT_STORAGE.save(event);
      EVENT_BUS.post(
          new AccountDebitedEvent(event.getAggregateUUID(), event.getFromUUID(), event.getToUUID(),
              event.getTransactionUUID(), event.getValue()));
    }
  }

  @Subscribe
  static void handle(AccountDebitedEvent event) {
    log.info("Received AccountDebitedEvent: {}", event);
    // Saves TransferMoneyEvent in receiver's aggregate
    ACCOUNT_EVENT_STORAGE.save(
        new TransferMoneyEvent(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
    // Requests crediting receiver's account
    EVENT_BUS.post(
        new CreditAccountEvent(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  static void handle(CreditAccountEvent event) {
    log.info("Received CreditAccountEvent: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);

    // Marks transfer as succeeded in issuer account
    EVENT_BUS.post(
        new SuccessMoneyTransfer(event.getFromUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));

    // Marks transfer as succeeded in receiver account
    EVENT_BUS.post(
        new SuccessMoneyTransfer(event.getToUUID(), event.getFromUUID(), event.getToUUID(),
            event.getTransactionUUID(),
            event.getValue()));
  }

  @Subscribe
  static void handle(SuccessMoneyTransfer event) {
    log.info("Received SuccessMoneyTransfer: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);
  }


  @Subscribe
  static void handle(CancelMoneyTransfer event) {
    log.info("Received CancelMoneyTransfer: {}", event);
    ACCOUNT_EVENT_STORAGE.save(event);
  }

}
