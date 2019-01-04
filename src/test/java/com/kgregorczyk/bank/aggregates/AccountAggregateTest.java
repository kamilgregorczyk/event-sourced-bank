package com.kgregorczyk.bank.aggregates;


import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountCreditedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountDebitedEvent;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferSucceeded;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountAggregateTest {

  private static final UUID FROM_UUID = UUID.randomUUID();
  private static final UUID TO_UUID = UUID.randomUUID();
  private static final AccountCreatedEvent ACCOUNT_CREATED = new AccountCreatedEvent(
      UUID.randomUUID(),
      "Kamil Gregorczyk");
  private static final FullNameChangedEvent FULL_NAME_CHANGED =
      new FullNameChangedEvent(UUID.randomUUID(), "Kamil Gregorczyk");
  private static final MoneyTransferredEvent ISSUER_MONEY_TRANSFERRED = new MoneyTransferredEvent(
      FROM_UUID, FROM_UUID,
      TO_UUID, UUID.randomUUID(), BigDecimal.TEN);
  private static final MoneyTransferredEvent RECEIVER_MONEY_TRANSFERRED =
      new MoneyTransferredEvent(
          ISSUER_MONEY_TRANSFERRED.getToUUID(), ISSUER_MONEY_TRANSFERRED.getFromUUID(),
          ISSUER_MONEY_TRANSFERRED.getToUUID(),
          ISSUER_MONEY_TRANSFERRED.getTransactionUUID(),
          ISSUER_MONEY_TRANSFERRED.getValue());
  private static final AccountDebitedEvent ACCOUNT_DEBITED = new AccountDebitedEvent(
      ISSUER_MONEY_TRANSFERRED.getAggregateUUID(),
      ISSUER_MONEY_TRANSFERRED.getFromUUID(),
      ISSUER_MONEY_TRANSFERRED.getToUUID(),
      ISSUER_MONEY_TRANSFERRED.getTransactionUUID(),
      ISSUER_MONEY_TRANSFERRED.getValue());
  private static final AccountCreditedEvent ACCOUNT_CREDITED = new AccountCreditedEvent(
      RECEIVER_MONEY_TRANSFERRED.getAggregateUUID(),
      RECEIVER_MONEY_TRANSFERRED.getFromUUID(),
      RECEIVER_MONEY_TRANSFERRED.getToUUID(),
      RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(),
      RECEIVER_MONEY_TRANSFERRED.getValue());
  private static final MoneyTransferCancelled ISSUER_MONEY_TRANSFER_CANCELLED = new MoneyTransferCancelled(
      ISSUER_MONEY_TRANSFERRED.getAggregateUUID(),
      ISSUER_MONEY_TRANSFERRED.getFromUUID(),
      ISSUER_MONEY_TRANSFERRED.getToUUID(),
      ISSUER_MONEY_TRANSFERRED.getTransactionUUID(),
      ISSUER_MONEY_TRANSFERRED.getValue(), Reason.BALANCE_TOO_LOW);
  private static final MoneyTransferCancelled RECEIVER_MONEY_TRANSFER_CANCELLED = new MoneyTransferCancelled(
      RECEIVER_MONEY_TRANSFERRED.getAggregateUUID(),
      RECEIVER_MONEY_TRANSFERRED.getFromUUID(),
      RECEIVER_MONEY_TRANSFERRED.getToUUID(),
      RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(),
      RECEIVER_MONEY_TRANSFERRED.getValue(), Reason.BALANCE_TOO_LOW);
  private static final MoneyTransferSucceeded ISSUER_MONEY_TRANSFER_SUCCEEDED = new MoneyTransferSucceeded(
      ISSUER_MONEY_TRANSFERRED.getAggregateUUID(),
      ISSUER_MONEY_TRANSFERRED.getFromUUID(),
      ISSUER_MONEY_TRANSFERRED.getToUUID(),
      ISSUER_MONEY_TRANSFERRED.getTransactionUUID(),
      ISSUER_MONEY_TRANSFERRED.getValue());
  private static final MoneyTransferSucceeded RECEIVER_MONEY_TRANSFER_SUCCEEDED =
      new MoneyTransferSucceeded(
          RECEIVER_MONEY_TRANSFERRED.getAggregateUUID(),
          RECEIVER_MONEY_TRANSFERRED.getFromUUID(),
          RECEIVER_MONEY_TRANSFERRED.getToUUID(),
          RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(),
          RECEIVER_MONEY_TRANSFERRED.getValue());

  @Test
  public void accountCreatedEvent() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList.of(ACCOUNT_CREATED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEmpty();
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void fullNameChangedEvent() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, FULL_NAME_CHANGED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(FULL_NAME_CHANGED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEmpty();
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(FULL_NAME_CHANGED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferredEventIssuer() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.NEW)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ISSUER_MONEY_TRANSFERRED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferredEventReceiver() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, RECEIVER_MONEY_TRANSFERRED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(RECEIVER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(RECEIVER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.NEW)
            .type(MoneyTransaction.Type.INCOMING)
            .value(RECEIVER_MONEY_TRANSFERRED.getValue())
            .createdAt(RECEIVER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(RECEIVER_MONEY_TRANSFERRED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(RECEIVER_MONEY_TRANSFERRED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void accountDebitedEvent() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED, ACCOUNT_DEBITED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(990).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.PENDING)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ACCOUNT_DEBITED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEqualTo(
        ImmutableMap.of(ACCOUNT_DEBITED.getTransactionUUID(), ACCOUNT_DEBITED.getValue().negate())
    );
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ACCOUNT_DEBITED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void accountDebitedEventNoTransaction() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ACCOUNT_DEBITED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(990).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEmpty();
    assertThat(aggregate.getTransactionToReservedBalance()).isEqualTo(
        ImmutableMap.of(ACCOUNT_DEBITED.getTransactionUUID(), ACCOUNT_DEBITED.getValue().negate())
    );
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ACCOUNT_DEBITED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void accountCreditedEvent() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, RECEIVER_MONEY_TRANSFERRED, ACCOUNT_CREDITED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(RECEIVER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(RECEIVER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.PENDING)
            .type(MoneyTransaction.Type.INCOMING)
            .value(RECEIVER_MONEY_TRANSFERRED.getValue())
            .createdAt(RECEIVER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ACCOUNT_CREDITED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEqualTo(
        ImmutableMap.of(ACCOUNT_CREDITED.getTransactionUUID(), ACCOUNT_CREDITED.getValue())
    );
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ACCOUNT_DEBITED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void accountCreditedEventWithNoTransaction() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ACCOUNT_CREDITED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEmpty();
    assertThat(aggregate.getTransactionToReservedBalance()).isEqualTo(
        ImmutableMap.of(ACCOUNT_CREDITED.getTransactionUUID(), ACCOUNT_CREDITED.getValue())
    );
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt()).isEqualTo(ACCOUNT_DEBITED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferSucceededNoReservedMoney() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED,
            ISSUER_MONEY_TRANSFER_SUCCEEDED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.SUCCEEDED)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ISSUER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(ISSUER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferSucceededIssuer() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED, ACCOUNT_DEBITED,
            ISSUER_MONEY_TRANSFER_SUCCEEDED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(990).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.SUCCEEDED)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ISSUER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(ISSUER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferSucceededReceiver() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, RECEIVER_MONEY_TRANSFERRED, ACCOUNT_CREDITED,
            RECEIVER_MONEY_TRANSFER_SUCCEEDED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1010).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(RECEIVER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(RECEIVER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.SUCCEEDED)
            .type(MoneyTransaction.Type.INCOMING)
            .value(RECEIVER_MONEY_TRANSFERRED.getValue())
            .createdAt(RECEIVER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(RECEIVER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(RECEIVER_MONEY_TRANSFER_SUCCEEDED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferCancelledEventIssuer() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED, ISSUER_MONEY_TRANSFER_CANCELLED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.CANCELLED)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ISSUER_MONEY_TRANSFER_CANCELLED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(ISSUER_MONEY_TRANSFER_CANCELLED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferCancelledEventIssuerWhenAccountWasDebited() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED, ACCOUNT_DEBITED,
            ISSUER_MONEY_TRANSFER_CANCELLED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(ISSUER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(ISSUER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(ISSUER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(ISSUER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.CANCELLED)
            .type(MoneyTransaction.Type.OUTGOING)
            .value(ISSUER_MONEY_TRANSFERRED.getValue().negate())
            .createdAt(ISSUER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(ISSUER_MONEY_TRANSFER_CANCELLED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(ISSUER_MONEY_TRANSFER_CANCELLED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }

  @Test
  public void moneyTransferCancelledEventReceiver() {
    // given
    ImmutableList<DomainEvent> events = ImmutableList
        .of(ACCOUNT_CREATED, RECEIVER_MONEY_TRANSFERRED, ACCOUNT_CREDITED,
            RECEIVER_MONEY_TRANSFER_CANCELLED);

    // when
    AccountAggregate aggregate = AccountEventStorage.recreate(events);

    // assert
    assertThat(aggregate.getFullName()).isEqualTo(ACCOUNT_CREATED.getFullName());
    assertThat(aggregate.getUuid()).isEqualTo(ACCOUNT_CREATED.getAggregateUUID());
    assertThat(aggregate.getBalance())
        .isEqualTo(BigDecimal.valueOf(1000).setScale(2, RoundingMode.HALF_EVEN));
    assertThat(aggregate.getTransactions()).isEqualTo(
        ImmutableMap.of(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID(), MoneyTransaction.builder()
            .fromUUID(RECEIVER_MONEY_TRANSFERRED.getFromUUID())
            .toUUID(RECEIVER_MONEY_TRANSFERRED.getToUUID())
            .transactionUUID(RECEIVER_MONEY_TRANSFERRED.getTransactionUUID())
            .state(State.CANCELLED)
            .type(MoneyTransaction.Type.INCOMING)
            .value(RECEIVER_MONEY_TRANSFERRED.getValue())
            .createdAt(RECEIVER_MONEY_TRANSFERRED.getCreatedAt())
            .lastUpdatedAt(RECEIVER_MONEY_TRANSFER_CANCELLED.getCreatedAt())
            .build())
    );
    assertThat(aggregate.getTransactionToReservedBalance()).isEmpty();
    assertThat(aggregate.getCreatedAt()).isEqualTo(ACCOUNT_CREATED.getCreatedAt());
    assertThat(aggregate.getLastUpdatedAt())
        .isEqualTo(RECEIVER_MONEY_TRANSFER_CANCELLED.getCreatedAt());
    assertThat(aggregate.getDomainEvents()).isEqualTo(events);
  }
}