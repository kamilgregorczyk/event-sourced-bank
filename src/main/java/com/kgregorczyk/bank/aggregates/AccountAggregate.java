package com.kgregorczyk.bank.aggregates;

import static com.kgregorczyk.bank.Singletons.EVENT_BUS;

import com.kgregorczyk.bank.aggregates.events.AccountCreatedEvent;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import com.kgregorczyk.bank.aggregates.events.FullNameChangedEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Account model
 */
@ToString
@Getter
@EqualsAndHashCode
public class AccountAggregate {

  private UUID uuid;
  private String fullName;
  private List<Update> updates;

  public static void createAccountCommand(String fullName) {
    if (fullName.isEmpty()) {
      throw new IllegalStateException("fullName cannot be empty!");
    }
    EVENT_BUS.post(new AccountCreatedEvent(UUID.randomUUID(), fullName, new Date()));
  }

  public static void changeFullNameCommand(UUID uuid, String fullName) {
    if (fullName.isEmpty()) {
      throw new IllegalStateException("fullName cannot be empty!");
    }
    EVENT_BUS.post(new FullNameChangedEvent(uuid, fullName, new Date()));
  }

  private void accountCreatedEventHandler(AccountCreatedEvent event) {
    uuid = event.getAggregateUUID();
    fullName = event.getFullName();
    updates = new ArrayList<>();
    updates.add(new Update("Account Created", event.getCreatedAt()));
  }

  private void fullNameChangedEventHandler(FullNameChangedEvent event) {
    fullName = event.getFullName();
    updates.add(new Update(String.format("Full name changed to: %s", event.getFullName()),
        event.getCreatedAt()));
  }

  static AccountAggregate recreate(List<DomainEvent> domainEvents) {
    AccountAggregate account = new AccountAggregate();
    for (DomainEvent event : domainEvents) {
      if (event instanceof AccountCreatedEvent) {
        account.accountCreatedEventHandler((AccountCreatedEvent) event);
      } else if (event instanceof FullNameChangedEvent) {
        account.fullNameChangedEventHandler((FullNameChangedEvent) event);
      }
    }
    return account;
  }
}
