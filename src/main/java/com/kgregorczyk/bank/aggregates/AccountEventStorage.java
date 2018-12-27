package com.kgregorczyk.bank.aggregates;

import com.google.common.collect.ImmutableList;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for events that make {@link AccountAggregate}.
 */
public class AccountEventStorage {

  private final Map<UUID, List<DomainEvent>> events = new ConcurrentHashMap<>();

  public void save(DomainEvent domainEvent) {
    List<DomainEvent> currentEvents = events
        .computeIfAbsent(domainEvent.getAggregateUUID(), uuid2 -> new ArrayList<>());
    currentEvents.add(domainEvent);
  }


  public ImmutableList<AccountAggregate> loadAll() {
    return events.entrySet()
        .stream()
        .parallel()
        .map(entry -> AccountAggregate.recreate(entry.getValue()))
        .collect(ImmutableList.toImmutableList());
  }

  public AccountAggregate loadByUUID(UUID uuid) {
    if (events.containsKey(uuid)) {
      return AccountAggregate.recreate(events.get(uuid));
    }
    throw new IllegalStateException(
        String.format("AccountAggregate with UUID: %s was not found!", uuid));
  }


  public boolean exists(UUID uuid) {
    return events.containsKey(uuid);
  }
}
