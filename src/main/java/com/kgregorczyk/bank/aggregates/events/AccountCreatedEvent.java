package com.kgregorczyk.bank.aggregates.events;

import java.util.Date;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountCreatedEvent extends DomainEvent {

  private String fullName;

  public AccountCreatedEvent(UUID aggregateUUID, String fullName) {
    super(aggregateUUID, new Date());
    this.fullName = fullName;
  }
}
