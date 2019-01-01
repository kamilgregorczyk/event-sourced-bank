package com.kgregorczyk.bank.aggregates;

import static io.vavr.API.Case;
import static io.vavr.API.Match.Pattern0.of;

import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountCreditedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountDebitedEvent;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferSucceeded;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import io.vavr.API;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * AccountAggregate is constructed based on events that are stored in {@link AccountEventStorage}.
 * Each stored event mutates the state which eventually leads into final object.
 *
 * <p>All operations that modify the aggregate from external packages must use commands which then
 * validate the input and asynchronously trigger event handlers which will store the mutation event
 * {@link DomainEvent} and trigger other actions if needed.
 *
 * <p>Each {@link DomainEvent} related to AccountAggregate mutates it in a specific order therefore
 * events must be stored in order and must be emitted by FIFO PubSub/EventBus for each aggregate.
 */
@ToString
@Getter
@EqualsAndHashCode
public class AccountAggregate {

  private static final double INITIAL_BALANCE = 1000;
  private UUID uuid;
  private String fullName;
  private BigDecimal balance;
  private Map<UUID, BigDecimal> transactionToReservedBalance;
  private List<DomainEvent> domainEvents;
  private Map<UUID, MoneyTransaction> transactions;

  AccountAggregate(List<DomainEvent> domainEvents) {
    this.domainEvents = domainEvents;
  }

  /**
   * Applies stored event by rerouting it to proper handler.
   */
  AccountAggregate apply(DomainEvent event) {
    return API.Match(event)
        .of(Case(of(AccountCreatedEvent.class), this::apply),
            Case(of(FullNameChangedEvent.class), this::apply),
            Case(of(MoneyTransferredEvent.class), this::apply),
            Case(of(AccountDebitedEvent.class), this::apply),
            Case(of(AccountCreditedEvent.class), this::apply),
            Case(of(MoneyTransferCancelled.class), this::apply),
            Case(of(MoneyTransferSucceeded.class), this::apply)
        );
  }

  AccountAggregate apply(AccountCreatedEvent event) {
    uuid = event.getAggregateUUID();
    transactionToReservedBalance = new TreeMap<>(); // TreeMap because it keeps the order
    balance = BigDecimal.valueOf(INITIAL_BALANCE).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    fullName = event.getFullName();
    transactions = new TreeMap<>(); // TreeMap because it keeps the order
    return this;
  }

  AccountAggregate apply(FullNameChangedEvent event) {
    fullName = event.getFullName();
    return this;
  }

  AccountAggregate apply(MoneyTransferredEvent event) {
    BigDecimal value;
    MoneyTransaction.Type type;

    if (event.getAggregateUUID().equals(event.getFromUUID())) {
      // Outgoing money transfer
      value = event.getValue().negate();
      type = MoneyTransaction.Type.OUTGOING;
    } else {
      // Incoming money transfer
      value = event.getValue();
      type = MoneyTransaction.Type.INCOMING;
    }
    transactions.put(event.getTransactionUUID(),
        new MoneyTransaction(event.getTransactionUUID(), event.getFromUUID(), event.getToUUID(),
            value, State.NEW, type, new Date(), new Date()));
    return this;
  }

  AccountAggregate apply(AccountDebitedEvent event) {
    if (balance.subtract(event.getValue()).compareTo(BigDecimal.ZERO) >= 0) {
      // Reserves balance for receiver
      balance = balance.subtract(event.getValue());
      transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue().negate());
      changeTransactionState(event.getTransactionUUID(), State.PENDING);
      return this;
    } else {
      throw new BalanceTooLowException();
    }

  }

  AccountAggregate apply(AccountCreditedEvent event) {
    // Adds a temp. balance
    transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue());
    changeTransactionState(event.getTransactionUUID(), State.PENDING);
    return this;
  }

  AccountAggregate apply(MoneyTransferSucceeded event) {
    transactionToReservedBalance.remove(event.getTransactionUUID());
    changeTransactionState(event.getTransactionUUID(), State.SUCCEEDED);

    // Increments receiver's account
    if (event.getToUUID().equals(event.getAggregateUUID())) {
      balance = balance.add(event.getValue());
    }
    return this;
  }

  AccountAggregate apply(MoneyTransferCancelled event) {

    if (event.getToUUID().equals(event.getAggregateUUID())) {
      // Cancelling money transfer for receiver receiver
      transactionToReservedBalance.remove(event.getTransactionUUID());
    } else {
      // Cancelling money transfer for issuer
      if (transactionToReservedBalance.containsKey(event.getTransactionUUID())) {
        BigDecimal reservedBalance = transactionToReservedBalance.get(event.getTransactionUUID());
        balance = balance.add(reservedBalance);
      }
      transactionToReservedBalance.remove(event.getTransactionUUID());
    }
    changeTransactionState(event.getTransactionUUID(), State.CANCELLED);
    return this;
  }

  private void changeTransactionState(UUID transactionUUID, State state) {
    MoneyTransaction transaction = transactions.get(transactionUUID);
    transaction.setState(state);
    transaction.setLastUpdatedAt(new Date());
  }
}
