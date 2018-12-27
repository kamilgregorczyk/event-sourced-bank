package com.kgregorczyk.bank.controllers.dto;

import com.kgregorczyk.bank.aggregates.AccountAggregate;
import com.kgregorczyk.bank.aggregates.Update;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Account {

  private String fullName;
  private UUID accountNumber;
  private BigDecimal balance;
  private List<Update> updates;

  public static Account from(AccountAggregate aggregate) {
    return Account.builder().accountNumber(aggregate.getUuid()).fullName(aggregate.getFullName())
        .updates(aggregate.getUpdates())
        .build();
  }
}
