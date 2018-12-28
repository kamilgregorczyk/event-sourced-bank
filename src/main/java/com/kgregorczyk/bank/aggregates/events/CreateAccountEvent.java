package com.kgregorczyk.bank.aggregates.events;

import java.util.Date;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateAccountEvent extends DomainEvent {

  private String fullName;

  public CreateAccountEvent(UUID aggregateUUID, String fullName) {
    super(aggregateUUID, new Date());
    this.fullName = fullName;
  }
}
