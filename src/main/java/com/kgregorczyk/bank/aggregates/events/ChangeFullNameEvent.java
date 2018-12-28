package com.kgregorczyk.bank.aggregates.events;

import java.util.Date;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChangeFullNameEvent extends DomainEvent {

  private String fullName;

  public ChangeFullNameEvent(UUID aggregateUUID, String fullName) {
    super(aggregateUUID, new Date());
    this.fullName = fullName;
  }
}
