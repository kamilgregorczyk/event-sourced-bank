package com.kgregorczyk.bank.controllers.dto;

import com.kgregorczyk.bank.aggregates.AccountAggregate;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.Type;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AccountDTO {

  private String fullName;
  private UUID accountNumber;
  private BigDecimal balance;
  private Map<UUID, BigDecimal> transactionToReservedBalance;
  private List<DomainEvent> events;
  private List<MoneyTransactionDTO> transactions;

  public static AccountDTO from(AccountAggregate aggregate) {
    return AccountDTO.builder().accountNumber(aggregate.getUuid()).fullName(aggregate.getFullName())
        .balance(aggregate.getBalance())
        .transactionToReservedBalance(aggregate.getTransactionToReservedBalance())
        .events(aggregate.getDomainEvents())
        .transactions(aggregate.getTransactions()
            .stream()
            .map(
                transaction -> MoneyTransactionDTO
                    .builder()
                    .transactionUUID(transaction.getTransactionUUID())
                    .fromAccountUUID(transaction.getFromUUID())
                    .toAccountUUID(transaction.getToUUID())
                    .value(transaction.getValue())
                    .state(transaction.getState())
                    .type(transaction.getType())
                    .lastUpdatedAt(transaction.getLastUpdatedAt())
                    .createdAt(transaction.getCreatedAt())
                    .build()
            )
            .collect(Collectors.toList()))
        .build();
  }

  @Getter
  @Builder
  private static class MoneyTransactionDTO {

    private UUID transactionUUID;
    private UUID fromAccountUUID;
    private UUID toAccountUUID;
    private BigDecimal value;
    private State state;
    private Type type;
    private Date lastUpdatedAt;
    private Date createdAt;

  }
}
