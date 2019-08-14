package com.kgregorczyk.bank.cron;

import com.google.common.collect.ImmutableList;
import com.kgregorczyk.bank.aggregates.AccountAggregate;
import com.kgregorczyk.bank.aggregates.AccountEventStorage;
import com.kgregorczyk.bank.aggregates.AccountService;
import com.kgregorczyk.bank.aggregates.events.*;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionRollbackCronTest {

  private static final Date DATE_IN_PAST =
      new Date(new Date().getTime() - (3 * 60 * 60 * 1000)); // 3 hours in past
  private static final AccountCreatedEvent ACCOUNT_CREATED_1 =
      new AccountCreatedEvent(UUID.randomUUID(), "A");
  private static final AccountCreatedEvent ACCOUNT_CREATED_2 =
      new AccountCreatedEvent(UUID.randomUUID(), "B");
  private static final AccountCreatedEvent ACCOUNT_CREATED_3 =
      new AccountCreatedEvent(UUID.randomUUID(), "C");
  private static final MoneyTransferredEvent MONEY_TRANSFERRED_1 =
      new MoneyTransferredEvent(
          ACCOUNT_CREATED_1.getAggregateUUID(),
          ACCOUNT_CREATED_1.getAggregateUUID(),
          ACCOUNT_CREATED_2.getAggregateUUID(),
          UUID.randomUUID(),
          BigDecimal.TEN);

  private static final MoneyTransferredEvent MONEY_TRANSFERRED_2 =
      new MoneyTransferredEvent(
          ACCOUNT_CREATED_2.getAggregateUUID(),
          ACCOUNT_CREATED_1.getAggregateUUID(),
          ACCOUNT_CREATED_2.getAggregateUUID(),
          MONEY_TRANSFERRED_1.getTransactionUUID(),
          BigDecimal.TEN);
  private static final MoneyTransferredEvent MONEY_TRANSFERRED_3 =
      new MoneyTransferredEvent(
          ACCOUNT_CREATED_3.getAggregateUUID(),
          ACCOUNT_CREATED_3.getAggregateUUID(),
          UUID.randomUUID(),
          UUID.randomUUID(),
          BigDecimal.TEN);
  private static final MoneyTransferredEvent MONEY_TRANSFERRED_1_IN_PAST =
      addPastDate(MONEY_TRANSFERRED_1);
  private static final MoneyTransferredEvent MONEY_TRANSFERRED_2_IN_PAST =
      addPastDate(MONEY_TRANSFERRED_2);
  private static final MoneyTransferredEvent MONEY_TRANSFERRED_3_IN_PAST =
      addPastDate(MONEY_TRANSFERRED_3);

  private static final MoneyTransferSucceeded MONEY_TRANSFER_SUCCEEDED_1 =
      moneyTransferSucceeded(MONEY_TRANSFERRED_1_IN_PAST);
  private static final MoneyTransferSucceeded MONEY_TRANSFER_SUCCEEDED_2 =
      moneyTransferSucceeded(MONEY_TRANSFERRED_2_IN_PAST);
  private static final MoneyTransferSucceeded MONEY_TRANSFER_SUCCEEDED_3 =
      moneyTransferSucceeded(MONEY_TRANSFERRED_3_IN_PAST);
  private static final MoneyTransferCancelled MONEY_TRANSFER_CANCELLED_1 =
      moneyTransferCancelled(MONEY_TRANSFERRED_1_IN_PAST);

  private static final MoneyTransferCancelled MONEY_TRANSFER_CANCELLED_2 =
      moneyTransferCancelled(MONEY_TRANSFERRED_2_IN_PAST);
  private static final MoneyTransferCancelled MONEY_TRANSFER_CANCELLED_3 =
      moneyTransferCancelled(MONEY_TRANSFERRED_3_IN_PAST);

  @Mock private AccountService accountService;

  @Mock private AccountEventStorage accountEventStorage;

  @InjectMocks private TransactionRollbackCron cron;

  private static MoneyTransferredEvent addPastDate(MoneyTransferredEvent event) {
    return new MoneyTransferredEvent(
        event.getAggregateUUID(),
        event.getFromUUID(),
        event.getToUUID(),
        event.getTransactionUUID(),
        event.getValue(),
        DATE_IN_PAST);
  }

  private static MoneyTransferSucceeded moneyTransferSucceeded(MoneyTransferredEvent event) {
    return new MoneyTransferSucceeded(
        event.getAggregateUUID(),
        event.getFromUUID(),
        event.getToUUID(),
        event.getTransactionUUID(),
        event.getValue(),
        event.getCreatedAt());
  }

  private static MoneyTransferCancelled moneyTransferCancelled(MoneyTransferredEvent event) {
    return new MoneyTransferCancelled(
        event.getAggregateUUID(),
        event.getFromUUID(),
        event.getToUUID(),
        event.getTransactionUUID(),
        event.getValue(),
        Reason.BALANCE_TOO_LOW,
        event.getCreatedAt());
  }

  private static AccountAggregate aggregate(DomainEvent... events) {
    return AccountEventStorage.recreate(Arrays.asList(events));
  }

  @Test
  public void transactionsModifiedRecentlyShouldNotBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1),
                aggregate(ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2),
                aggregate(ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3)));

    // when
    cron.run();

    // assert
    verifyZeroInteractions(accountService);
  }

  @Test
  public void transactionsNotModifiedRecentlyShouldBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST),
                aggregate(ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST),
                aggregate(ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST)));

    // when
    cron.run();

    // assert
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_1_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);

    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_2_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);

    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_3_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verifyNoMoreInteractions(accountService);
  }

  @Test
  public void succeededTransactionsNotModifiedRecentlyShouldNotBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(
                    ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST, MONEY_TRANSFER_SUCCEEDED_1),
                aggregate(
                    ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST, MONEY_TRANSFER_SUCCEEDED_2),
                aggregate(
                    ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST, MONEY_TRANSFER_SUCCEEDED_3)));

    // when
    cron.run();

    // assert
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFER_SUCCEEDED_3.getAggregateUUID(),
            MONEY_TRANSFER_SUCCEEDED_3.getFromUUID(),
            MONEY_TRANSFER_SUCCEEDED_3.getToUUID(),
            MONEY_TRANSFER_SUCCEEDED_3.getTransactionUUID(),
            MONEY_TRANSFER_SUCCEEDED_3.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verifyNoMoreInteractions(accountService);
  }

  @Test
  public void cancelledTransactionsNotModifiedRecentlyShouldNotBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(
                    ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST, MONEY_TRANSFER_CANCELLED_1),
                aggregate(
                    ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST, MONEY_TRANSFER_CANCELLED_2),
                aggregate(
                    ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST, MONEY_TRANSFER_CANCELLED_3)));

    // when
    cron.run();

    // assert
    verifyZeroInteractions(accountService);
  }

  @Test
  public void transactionsWithMixedStatuesNotModifiedRecentlyShouldBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(
                    ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST, MONEY_TRANSFER_CANCELLED_1),
                aggregate(ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST),
                aggregate(ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST)));

    // when
    cron.run();

    // assert
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_2_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_2_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);

    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_3_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verifyNoMoreInteractions(accountService);
  }

  @Test
  public void transactionsWithMixedStatuesNotModifiedRecentlyShouldBeProcessed2() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST),
                aggregate(
                    ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST, MONEY_TRANSFER_CANCELLED_2),
                aggregate(
                    ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST, MONEY_TRANSFER_CANCELLED_3)));

    // when
    cron.run();

    // assert
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_1_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verifyNoMoreInteractions(accountService);
  }

  @Test
  public void transactionWithSucceededStateAndCancelledShouldBeProcessed() {
    // given
    when(accountEventStorage.findAll())
        .thenReturn(
            ImmutableList.of(
                aggregate(
                    ACCOUNT_CREATED_1, MONEY_TRANSFERRED_1_IN_PAST, MONEY_TRANSFER_SUCCEEDED_1),
                aggregate(
                    ACCOUNT_CREATED_2, MONEY_TRANSFERRED_2_IN_PAST, MONEY_TRANSFER_CANCELLED_2),
                aggregate(
                    ACCOUNT_CREATED_3, MONEY_TRANSFERRED_3_IN_PAST, MONEY_TRANSFER_SUCCEEDED_3)));

    // when
    cron.run();

    // assert
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_1_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_1_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verify(accountService)
        .asyncCancelTransactionCommand(
            MONEY_TRANSFERRED_3_IN_PAST.getAggregateUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getFromUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getToUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getTransactionUUID(),
            MONEY_TRANSFERRED_3_IN_PAST.getValue(),
            Reason.INTERNAL_SERVER_ERROR);
    verifyNoMoreInteractions(accountService);
  }
}
