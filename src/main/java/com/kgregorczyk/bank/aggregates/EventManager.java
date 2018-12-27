package com.kgregorczyk.bank.aggregates;

import com.google.common.eventbus.Subscribe;
import com.kgregorczyk.bank.Singletons;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener and dispatcher of events in whole system. It contains dependencies between events.
 */
@Slf4j
public class EventManager {

  @Subscribe
  public void domainEventHandler(DomainEvent event) {
    log.debug("Received DomainEvent: {}", event);
    Singletons.ACCOUNT_EVENT_STORAGE.save(event);
  }
}
