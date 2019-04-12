package com.kgregorczyk.bank.aggregates;

import com.google.common.collect.ImmutableList;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.vavr.collection.List.ofAll;

/** In-memory storage for events that make {@link AccountAggregate}. */
public class AccountEventStorage {

  private final Map<UUID, List<DomainEvent>> events = new ConcurrentHashMap<>();

  public static AccountAggregate recreate(List<DomainEvent> events) {
    return ofAll(events).foldLeft(new AccountAggregate(events), (AccountAggregate::apply));
  }

  public ImmutableList<AccountAggregate> loadAll() {
    return events.entrySet().stream()
        .parallel()
        .map(entry -> recreate(entry.getValue()))
        .collect(ImmutableList.toImmutableList());
  }

  public AccountAggregate loadByUUID(UUID uuid) {
    if (events.containsKey(uuid)) {
      return recreate(events.get(uuid));
    }
    return null;
  }

  public boolean exists(UUID uuid) {
    return events.containsKey(uuid);
  }

  public void save(DomainEvent domainEvent) {
    List<DomainEvent> currentEvents =
        events.computeIfAbsent(domainEvent.getAggregateUUID(), uuid2 -> new ArrayList<>());
    currentEvents.add(domainEvent);
  }
}
