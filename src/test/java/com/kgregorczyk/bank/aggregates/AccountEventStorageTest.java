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
    AccountCreatedEvent event1 = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    AccountCreatedEvent event2 = new AccountCreatedEvent(UUID.randomUUID(), "Black Widow");
    AccountEventStorage storage = new AccountEventStorage();
    storage.save(event1);
    storage.save(event2);

    // when
    ImmutableList<AccountAggregate> aggregates = storage.loadAll();
    // assert
    assertThat(aggregates).containsExactly(
        AccountEventStorage.recreate(ImmutableList.of(event1)), AccountEventStorage
            .recreate(ImmutableList.of(event2)));

  }

  @Test
  void loadByUUIDExistingAggregate() {
    // given
    AccountCreatedEvent event = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    AccountEventStorage storage = new AccountEventStorage();
    storage.save(event);

    // when
    AccountAggregate aggregate = storage.loadByUUID(event.getAggregateUUID());

    // assert
    assertThat(aggregate).isEqualTo(AccountEventStorage.recreate(ImmutableList.of(event)));

  }

  @Test
  void loadByUUIDNotExistingAggregate() {
    // given
    AccountEventStorage storage = new AccountEventStorage();

    // when & assert
    assertThat(storage.loadByUUID(UUID.randomUUID())).isNull();

  }

  @Test
  void existsExistingAggregate() {
    // given
    AccountCreatedEvent event = new AccountCreatedEvent(UUID.randomUUID(), "Tony Stark");
    AccountEventStorage storage = new AccountEventStorage();
    storage.save(event);

    // when & assert
    assertThat(storage.exists(event.getAggregateUUID())).isTrue();

  }

  @Test
  void existsNotExistingAggregate() {
    // given
    AccountEventStorage storage = new AccountEventStorage();

    // when & assert
    assertThat(storage.exists(UUID.randomUUID())).isFalse();

  }
}