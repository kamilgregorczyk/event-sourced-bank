package com.kgregorczyk.bank.aggregates;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountEventStorageTest {

  @Test
  void loadAll() {
    // given
    var event1 = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    var event2 = new AccountCreatedEvent(UUID.randomUUID(), "Black Widow");
    var storage = new AccountEventStorage();
    storage.save(event1);
    storage.save(event2);

    // when
    var aggregates = storage.findAll();
    // assert
    assertThat(aggregates)
        .containsExactly(
            AccountEventStorage.recreate(ImmutableList.of(event1)),
            AccountEventStorage.recreate(ImmutableList.of(event2)));
  }

  @Test
  void loadByUUIDExistingAggregate() {
    // given
    var event = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    var storage = new AccountEventStorage();
    storage.save(event);

    // when
    var aggregate = storage.get(event.getAggregateUUID());

    // assert
    assertThat(aggregate).isEqualTo(AccountEventStorage.recreate(ImmutableList.of(event)));
  }

  @Test
  void loadByUUIDNotExistingAggregate() {
    // given
    var storage = new AccountEventStorage();

    // when & assert
    assertThat(storage.get(UUID.randomUUID())).isNull();
  }

  @Test
  void existsExistingAggregate() {
    // given
    var event = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    var storage = new AccountEventStorage();
    storage.save(event);

    // when & assert
    assertThat(storage.exists(event.getAggregateUUID())).isTrue();
  }

  @Test
  void existsNotExistingAggregate() {
    // given
    var storage = new AccountEventStorage();

    // when & assert
    assertThat(storage.exists(UUID.randomUUID())).isFalse();
  }
}
