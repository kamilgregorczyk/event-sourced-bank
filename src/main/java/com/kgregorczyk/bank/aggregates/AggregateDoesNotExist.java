package com.kgregorczyk.bank.aggregates;

import com.kgregorczyk.bank.aggregates.events.DomainEvent;

/**
 * Exception that's thrown when {@link DomainEvent} cannot be persisted.
 */
class AggregateDoesNotExist extends RuntimeException {

  AggregateDoesNotExist(String message) {
    super(message);
  }

}
