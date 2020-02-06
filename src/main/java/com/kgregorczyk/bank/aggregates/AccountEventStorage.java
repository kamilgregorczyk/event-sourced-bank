package com.kgregorczyk.bank.aggregates;

import static io.vavr.collection.List.ofAll;

import com.google.common.collect.ImmutableList;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for events that make {@link AccountAggregate}. */
public class AccountEventStorage {

  private final Map<UUID, List<DomainEvent>> events = new ConcurrentHashMap<>();

  public static AccountAggregate recreate(Collection<DomainEvent> events) {
    return ofAll(events).foldLeft(new AccountAggregate(events), (AccountAggregate::apply));
  }

  public ImmutableList<AccountAggregate> findAll() {
    return events.values().stream()
        .map(AccountEventStorage::recreate)
        .collect(ImmutableList.toImmutableList());
  }

  public AccountAggregate get(UUID uuid) {
    if (events.containsKey(uuid)) {
      return recreate(events.get(uuid));
    }
    return null;
  }

  public boolean exists(UUID uuid) {
    return events.containsKey(uuid);
  }

  public void save(DomainEvent domainEvent) {
    events.compute(
        domainEvent.getAggregateUUID(),
        (id, events) ->
            (events == null)
                ? ImmutableList.of(domainEvent)
                : new ImmutableList.Builder<DomainEvent>().addAll(events).add(domainEvent).build());
  }
}
