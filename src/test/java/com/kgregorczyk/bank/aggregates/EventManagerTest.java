package com.kgregorczyk.bank.aggregates;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountCreditedEvent;
import com.kgregorczyk.bank.aggregates.events.AccountDebitedEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferSucceeded;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferredEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventManagerTest {

  private static final UUID FROM_UUID = UUID.randomUUID();
  private static final UUID TO_UUID = UUID.randomUUID();
  private static final AccountCreatedEvent ACCOUNT_CREATED = new AccountCreatedEvent(
      UUID.randomUUID(),
      "Tony Stark");
  private static final FullNameChangedEvent FULL_NAME_CHANGED =
      new FullNameChangedEvent(UUID.randomUUID(), "Tony Stark");
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
  private static final MoneyTransferCancelled MONEY_TRANSFER_CANCELLED = new MoneyTransferCancelled(
      ISSUER_MONEY_TRANSFERRED.getAggregateUUID(),
      ISSUER_MONEY_TRANSFERRED.getFromUUID(),
      ISSUER_MONEY_TRANSFERRED.getToUUID(),
      ISSUER_MONEY_TRANSFERRED.getTransactionUUID(),
      ISSUER_MONEY_TRANSFERRED.getValue(), Reason.BALANCE_TOO_LOW);
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


  @Mock
  private EventBus eventBus;

  @Mock
  private AccountEventStorage accountEventStorage;

  @InjectMocks
  private EventManager eventManager;

  @Test
  public void accountCreatedEvent() {
    // when
    eventManager.handle(ACCOUNT_CREATED);

    // assert
    verify(accountEventStorage).save(ACCOUNT_CREATED);
  }

  @Test
  public void changedFullNameEventAggregateExists() {
    // given

    when(accountEventStorage.exists(any())).thenReturn(true);

    // when
    eventManager.handle(FULL_NAME_CHANGED);

    // assert
    verify(accountEventStorage).save(FULL_NAME_CHANGED);
  }

  @Test
  public void changedFullNameEventAggregateDoesNotExist() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(false);

    // when
    assertThrows(AggregateDoesNotExist.class, () -> eventManager.handle(FULL_NAME_CHANGED));

    // assert
    verify(accountEventStorage).exists(FULL_NAME_CHANGED.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
  }

  @Test
  public void moneyTransferredEventAggregateExists() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(true);

    // when
    eventManager.handle(ISSUER_MONEY_TRANSFERRED);

    // assert
    verify(accountEventStorage).save(ISSUER_MONEY_TRANSFERRED);
    verify(eventBus).post(ACCOUNT_DEBITED);
  }

  @Test
  public void moneyTransferredEventAggregateDoesNotExist() {
    // given

    when(accountEventStorage.exists(any())).thenReturn(false);

    // when
    assertThrows(AggregateDoesNotExist.class, () -> eventManager.handle(
        ISSUER_MONEY_TRANSFERRED));

    // assert
    verify(accountEventStorage).exists(ISSUER_MONEY_TRANSFERRED.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
    verifyZeroInteractions(eventBus);
  }

  @Test
  public void accountDebitedEventAggregateExists() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(true);
    when(accountEventStorage.loadByUUID(ACCOUNT_DEBITED.getAggregateUUID()))
        .thenReturn(AccountEventStorage.recreate(
            ImmutableList.of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED)));

    // when
    eventManager.handle(ACCOUNT_DEBITED);

    // assert
    verify(accountEventStorage).save(ACCOUNT_DEBITED);
    verify(accountEventStorage).save(RECEIVER_MONEY_TRANSFERRED);
    verify(eventBus).post(ACCOUNT_CREDITED);
  }

  @Test
  public void accountDebitedEventAggregateDoesNotExist() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(false);
    when(accountEventStorage.loadByUUID(ACCOUNT_DEBITED.getAggregateUUID()))
        .thenReturn(null);

    // when
    assertThrows(AggregateDoesNotExist.class, () -> eventManager.handle(ACCOUNT_DEBITED));

    // assert
    verify(accountEventStorage).loadByUUID(ACCOUNT_DEBITED.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
    verifyZeroInteractions(eventBus);
  }

  @Test
  public void accountDebitedEventAggregateExistsNotEnoughMoney() {
    // given
    AccountDebitedEvent accountDebited =
        ACCOUNT_DEBITED.toBuilder().value(BigDecimal.valueOf(2000)).build();
    MoneyTransferCancelled moneyTransferCancelled =
        MONEY_TRANSFER_CANCELLED.toBuilder().value(accountDebited.getValue()).build();
    when(accountEventStorage.exists(any())).thenReturn(true);
    when(accountEventStorage.loadByUUID(ACCOUNT_DEBITED.getAggregateUUID()))
        .thenReturn(AccountEventStorage.recreate(
            ImmutableList.of(ACCOUNT_CREATED, ISSUER_MONEY_TRANSFERRED)));

    // when
    eventManager.handle(accountDebited);

    // assert
    verify(accountEventStorage).loadByUUID(ACCOUNT_DEBITED.getAggregateUUID());
    verify(eventBus).post(moneyTransferCancelled);
    verifyNoMoreInteractions(accountEventStorage);
    verifyNoMoreInteractions(eventBus);
  }

  @Test
  public void accountCreditedEventAggregateExists() {
    // given
    when(accountEventStorage.exists(ACCOUNT_CREDITED.getAggregateUUID())).thenReturn(true);

    // when
    eventManager.handle(ACCOUNT_CREDITED);

    // assert
    verify(accountEventStorage).save(ACCOUNT_CREDITED);
    verify(eventBus).post(ISSUER_MONEY_TRANSFER_SUCCEEDED);
    verify(eventBus).post(RECEIVER_MONEY_TRANSFER_SUCCEEDED);
  }

  @Test
  public void moneyTransferCancelledEventAggregateExists() {
    // given

    when(accountEventStorage.exists(any())).thenReturn(true);

    // when
    eventManager.handle(MONEY_TRANSFER_CANCELLED);

    // assert
    verify(accountEventStorage).save(MONEY_TRANSFER_CANCELLED);
  }

  @Test
  public void moneyTransferCancelledEventAggregateDoesNotExist() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(false);

    // when
    assertThrows(AggregateDoesNotExist.class, () -> eventManager.handle(MONEY_TRANSFER_CANCELLED));

    // assert
    verify(accountEventStorage).exists(MONEY_TRANSFER_CANCELLED.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
  }

  @Test
  public void moneyTransferSucceededEventAggregateExists() {
    // given

    when(accountEventStorage.exists(any())).thenReturn(true);

    // when
    eventManager.handle(ISSUER_MONEY_TRANSFER_SUCCEEDED);

    // assert
    verify(accountEventStorage).save(ISSUER_MONEY_TRANSFER_SUCCEEDED);
  }

  @Test
  public void moneyTransferSucceededEventAggregateDoesNotExist() {
    // given
    when(accountEventStorage.exists(any())).thenReturn(false);

    // when
    assertThrows(AggregateDoesNotExist.class,
        () -> eventManager.handle(ISSUER_MONEY_TRANSFER_SUCCEEDED));

    // assert
    verify(accountEventStorage).exists(ISSUER_MONEY_TRANSFER_SUCCEEDED.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
  }

}