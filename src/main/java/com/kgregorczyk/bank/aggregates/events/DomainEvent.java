package com.kgregorczyk.bank.aggregates.events;

import com.google.common.base.CaseFormat;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Base event for all the other events that mutate aggregates and that should be stored. */
@Getter
@ToString
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
public abstract class DomainEvent {

  Date createdAt;
  private String eventType;
  private UUID aggregateUUID;

  DomainEvent(UUID aggregateUUID, Date createdAt) {
    this.aggregateUUID = aggregateUUID;
    this.createdAt = createdAt;
    this.eventType = classNameToUpperCase();
  }

  DomainEvent() {
    this.eventType = classNameToUpperCase();
  }

  /** Converts for e.g. AccountCreatedEvent -> ACCOUNT_CREATED_EVENT */
  private String classNameToUpperCase() {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, this.getClass().getSimpleName());
  }
}
