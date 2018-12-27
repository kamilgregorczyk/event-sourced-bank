package com.kgregorczyk.bank.aggregates.events;

import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent extends DomainEvent {

  private String fullName;

  public AccountCreatedEvent(UUID uuid, String fullName, Date date) {
    super(uuid, date);
    this.fullName = fullName;
  }
}
