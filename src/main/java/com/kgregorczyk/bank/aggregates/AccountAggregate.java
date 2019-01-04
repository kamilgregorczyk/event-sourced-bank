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
import java.math.RoundingMode;
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
  private Date createdAt;
  private Date lastUpdatedAt;

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

  /**
   * Event that initializes aggregate.
   *
   * <p>Sets balance to {@link AccountAggregate#INITIAL_BALANCE} for simplicity.
   */
  AccountAggregate apply(AccountCreatedEvent event) {
    uuid = event.getAggregateUUID();
    transactionToReservedBalance = new TreeMap<>(); // TreeMap because it keeps the order
    //TODO: Replace with 0 initial balance.
    balance = BigDecimal.valueOf(INITIAL_BALANCE).setScale(2, RoundingMode.HALF_EVEN);
    fullName = event.getFullName();
    transactions = new TreeMap<>(); // TreeMap because it keeps the order
    createdAt = event.getCreatedAt();
    lastUpdatedAt = event.getCreatedAt();
    return this;
  }

  /**
   * Updates {@link AccountAggregate#fullName}.
   */
  AccountAggregate apply(FullNameChangedEvent event) {
    lastUpdatedAt = event.getCreatedAt();
    fullName = event.getFullName();
    return this;
  }

  /**
   * Appends new transaction to {@link AccountAggregate#transactions} with {@link
   * MoneyTransaction.State#NEW}.
   *
   * <p>If the event is applied on the issuer aggregate (account from money should be subtracted)
   * then the transaction is set to {@link MoneyTransaction.Type#OUTGOING} with negated value,
   * otherwise it's set to {@link MoneyTransaction.Type#INCOMING} with raw value.
   */
  AccountAggregate apply(MoneyTransferredEvent event) {
    BigDecimal value;
    MoneyTransaction.Type type;
    lastUpdatedAt = event.getCreatedAt();
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
            value, State.NEW, type, event.getCreatedAt(), event.getCreatedAt()));

    return this;
  }

  /**
   * Reserves balance on account that's about to have it's balance transferred and subtracts that
   * amount from the main balance.
   *
   * <p>If the event was followed by {@link MoneyTransferredEvent} (there's a transaction)
   * then it also updates the transaction state to {@link MoneyTransaction.State#PENDING}.
   *
   * @throws BalanceTooLowException when {@link AccountAggregate#balance} is not sufficient.
   */
  AccountAggregate apply(AccountDebitedEvent event) {
    if (balance.subtract(event.getValue()).compareTo(BigDecimal.ZERO) >= 0) {
      lastUpdatedAt = event.getCreatedAt();
      // Reserves balance for receiver
      balance = balance.subtract(event.getValue());
      transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue().negate());
      if (transactions.containsKey(event.getTransactionUUID())) {
        changeTransactionState(event.getTransactionUUID(), State.PENDING, event.getCreatedAt());
      }
      return this;
    } else {
      throw new BalanceTooLowException();
    }

  }

  /**
   * Reserves balance on account that's about to have it's balance increased (receiver or the money
   * transfer).
   *
   * <p>If the event was followed by {@link MoneyTransferredEvent} (there's a transaction)
   * then it also updates the transaction state to {@link MoneyTransaction.State#PENDING}.
   */
  AccountAggregate apply(AccountCreditedEvent event) {
    lastUpdatedAt = event.getCreatedAt();
    // Adds a temp. balance
    transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue());
    if (transactions.containsKey(event.getTransactionUUID())) {
      changeTransactionState(event.getTransactionUUID(), State.PENDING, event.getCreatedAt());
    }
    return this;
  }

  /**
   * Marks transaction as {@link MoneyTransaction.State#SUCCEEDED}. Releases reserved balance and
   * increments it for receiver of the money transfer.
   */
  AccountAggregate apply(MoneyTransferSucceeded event) {
    lastUpdatedAt = event.getCreatedAt();
    changeTransactionState(event.getTransactionUUID(), State.SUCCEEDED, event.getCreatedAt());
    if (transactionToReservedBalance.containsKey(event.getTransactionUUID())) {
      BigDecimal reservedMoney = transactionToReservedBalance.remove(event.getTransactionUUID());
      // Increments receiver's account
      if (event.getToUUID().equals(event.getAggregateUUID())) {
        balance = balance.add(reservedMoney);
      }
    }
    return this;
  }

  /**
   * Cancels ongoing transaction.
   *
   * <p>This event cannot be called once {@link  MoneyTransferSucceeded} has been called for the
   * same {@link MoneyTransaction#getTransactionUUID()}</p>
   */
  AccountAggregate apply(MoneyTransferCancelled event) {
    lastUpdatedAt = event.getCreatedAt();
    if (event.getToUUID().equals(event.getAggregateUUID())) {
      // Cancelling money transfer for receiver
      transactionToReservedBalance.remove(event.getTransactionUUID());
    } else if (transactionToReservedBalance.containsKey(event.getTransactionUUID())) {
      BigDecimal reservedBalance = transactionToReservedBalance.get(event.getTransactionUUID());
      balance = balance.add(reservedBalance.negate());
      transactionToReservedBalance.remove(event.getTransactionUUID());
    }

    changeTransactionState(event.getTransactionUUID(), State.CANCELLED, event.getCreatedAt());
    return this;
  }

  private void changeTransactionState(UUID transactionUUID, State state, Date lastUpdatedAt) {
    MoneyTransaction transaction = transactions.get(transactionUUID);
    transaction.setState(state);
    transaction.setLastUpdatedAt(lastUpdatedAt);
  }
}
