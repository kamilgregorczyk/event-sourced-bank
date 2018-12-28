package com.kgregorczyk.bank.aggregates;

import static com.kgregorczyk.bank.Singletons.EVENT_BUS;
import static io.vavr.API.Case;
import static io.vavr.API.Match.Pattern0.of;

import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.events.CancelMoneyTransfer;
import com.kgregorczyk.bank.aggregates.events.ChangeFullNameEvent;
import com.kgregorczyk.bank.aggregates.events.CreateAccountEvent;
import com.kgregorczyk.bank.aggregates.events.CreditAccountEvent;
import com.kgregorczyk.bank.aggregates.events.DebitAccountEvent;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import com.kgregorczyk.bank.aggregates.events.SuccessMoneyTransfer;
import com.kgregorczyk.bank.aggregates.events.TransferMoneyEvent;
import io.vavr.API;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * AccountDTO model
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
  private List<MoneyTransaction> transactions;

  public AccountAggregate(List<DomainEvent> domainEvents) {
    this.domainEvents = domainEvents;
  }

  // Command Handlers - entry points for interactions with aggregates from external classes
  public static void createAccountCommand(String fullName) {
    EVENT_BUS.post(new CreateAccountEvent(UUID.randomUUID(), fullName));
  }

  public static void changeFullNameCommand(UUID uuid, String fullName) {
    EVENT_BUS.post(new ChangeFullNameEvent(uuid, fullName));
  }

  public static void transferMoneyCommand(UUID fromUUID, UUID toUUID, BigDecimal value) {
    EVENT_BUS.post(new TransferMoneyEvent(fromUUID, fromUUID, toUUID, UUID.randomUUID(), value));
  }

  /**
   * Applies stored event by rerouting it to proper handler.
   */
  AccountAggregate apply(DomainEvent event) {
    return API.Match(event)
        .of(Case(of(CreateAccountEvent.class), this::apply),
            Case(of(ChangeFullNameEvent.class), this::apply),
            Case(of(TransferMoneyEvent.class), this::apply),
            Case(of(DebitAccountEvent.class), this::apply),
            Case(of(CreditAccountEvent.class), this::apply),
            Case(of(CancelMoneyTransfer.class), this::apply),
            Case(of(SuccessMoneyTransfer.class), this::apply)
        );
  }

  AccountAggregate apply(CreateAccountEvent event) {
    uuid = event.getAggregateUUID();
    transactionToReservedBalance = new HashMap<>();
    balance = BigDecimal.valueOf(INITIAL_BALANCE).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    fullName = event.getFullName();
    transactions = new ArrayList<>();
    return this;
  }

  AccountAggregate apply(ChangeFullNameEvent event) {
    fullName = event.getFullName();
    return this;
  }

  AccountAggregate apply(TransferMoneyEvent event) {
    BigDecimal value;
    MoneyTransaction.Type type;

    // Outgoing money transfer
    if (event.getAggregateUUID().equals(event.getFromUUID())) {
      value = event.getValue().negate();
      type = MoneyTransaction.Type.OUTGOING;
    } else {
      value = event.getValue();
      type = MoneyTransaction.Type.INCOMING;
    }
    transactions.add(
        new MoneyTransaction(event.getTransactionUUID(), event.getFromUUID(), event.getToUUID(),
            value, State.NEW, type, new Date(), new Date()));
    return this;
  }

  AccountAggregate apply(DebitAccountEvent event) {
    // Reserves balance for receiver
    balance = balance.subtract(event.getValue());
    transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue().negate());
    changeTransactionState(event.getTransactionUUID(), State.PENDING);
    return this;
  }

  AccountAggregate apply(CreditAccountEvent event) {
    // Adds a temp. balance
    transactionToReservedBalance.put(event.getTransactionUUID(), event.getValue());
    changeTransactionState(event.getTransactionUUID(), State.PENDING);
    return this;
  }


  AccountAggregate apply(SuccessMoneyTransfer event) {
    transactionToReservedBalance.remove(event.getTransactionUUID());
    changeTransactionState(event.getTransactionUUID(), State.SUCCEEDED);

    // Increments receiver's account
    if (event.getToUUID().equals(event.getAggregateUUID())) {
      balance = balance.add(event.getValue());
    }
    return this;
  }

  AccountAggregate apply(CancelMoneyTransfer event) {

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
    MoneyTransaction moneyTransaction = transactions.stream()
        .filter(transaction -> transaction.getTransactionUUID().equals(transactionUUID))
        .findFirst().get();
    moneyTransaction.setState(state);
    moneyTransaction.setLastUpdatedAt(new Date());
  }
}
