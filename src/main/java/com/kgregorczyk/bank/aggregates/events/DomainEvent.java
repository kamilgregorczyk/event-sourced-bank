package com.kgregorczyk.bank.aggregates.events;

import com.google.common.base.CaseFormat;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Base event for all the other events that mutate aggregates and that should be stored.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public abstract class DomainEvent {

  // Converts for e.g. CreateAccountEvent -> ACCOUNT_CREATED_EVENT
  private final String eventType = CaseFormat.UPPER_CAMEL
      .to(CaseFormat.UPPER_UNDERSCORE, this.getClass().getSimpleName());
  private UUID aggregateUUID;
  private Date createdAt;

}
