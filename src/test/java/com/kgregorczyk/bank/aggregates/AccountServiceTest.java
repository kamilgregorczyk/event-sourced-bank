package com.kgregorczyk.bank.aggregates;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

  @Mock private EventBus eventBus;

  @InjectMocks private AccountService service;

  @Test
  public void asyncCancelMoneyTransferCommand() {
    // given
    var aggregateUUID = UUID.randomUUID();
    var fromUUID = UUID.randomUUID();
    var toUUID = UUID.randomUUID();
    var transactionUUID = UUID.randomUUID();
    var value = BigDecimal.TEN;
    var reason = Reason.INTERNAL_SERVER_ERROR;

    // when
    service.asyncCancelTransactionCommand(
        aggregateUUID, fromUUID, toUUID, transactionUUID, value, reason);

    // assert
    verify(eventBus)
        .post(
            new MoneyTransferCancelled(
                aggregateUUID, fromUUID, toUUID, transactionUUID, value, reason));
  }
}
