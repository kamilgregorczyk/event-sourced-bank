package com.kgregorczyk.bank.aggregates.events;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class DomainEventTest {

  @Test
  public void testEventTypeConversion() {
    assertThat(new DummyEvent().getEventType()).isEqualTo("DUMMY_EVENT");
  }

  private static final class DummyEvent extends DomainEvent {}
}
