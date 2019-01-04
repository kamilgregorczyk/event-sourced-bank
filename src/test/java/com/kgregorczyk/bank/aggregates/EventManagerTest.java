package com.kgregorczyk.bank.aggregates;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
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

  @Mock
  private EventBus eventBus;

  @Mock
  private AccountEventStorage accountEventStorage;

  @InjectMocks
  private EventManager eventManager;

  @Test
  public void accountCreatedEvent() {
    // given
    AccountCreatedEvent event = new AccountCreatedEvent(UUID.randomUUID(), "Kamil Gregorczyk");

    // when
    eventManager.handle(event);

    // assert
    verify(accountEventStorage).save(event);
  }

  @Test
  public void changedFullNameEventAggregateExists() {
    // given
    AccountCreatedEvent event = new AccountCreatedEvent(UUID.randomUUID(), "Kamil Gregorczyk");
    when(accountEventStorage.exists(any())).thenReturn(true);

    // when
    eventManager.handle(event);

    // assert
    verify(accountEventStorage).save(event);
  }

  @Test
  public void fullNameChangedEventAggregateDoesNotExist() {
    // given
    FullNameChangedEvent event = new FullNameChangedEvent(UUID.randomUUID(), "Kamil Gregorczyk");
    when(accountEventStorage.exists(any())).thenReturn(false);

    // when
    assertThrows(AggregateDoesNotExist.class, () -> eventManager.handle(event));

    // assert
    verify(accountEventStorage).exists(event.getAggregateUUID());
    verifyNoMoreInteractions(accountEventStorage);
  }
}