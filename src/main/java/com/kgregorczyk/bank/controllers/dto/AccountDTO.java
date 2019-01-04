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
  private List<? extends DomainEvent> events;
  private Map<UUID, MoneyTransactionDTO> transactions;
  private Date createdAt;
  private Date lastUpdatedAt;

  public static AccountDTO from(AccountAggregate aggregate) {
    return AccountDTO.builder().accountNumber(aggregate.getUuid()).fullName(aggregate.getFullName())
        .balance(aggregate.getBalance())
        .transactionToReservedBalance(aggregate.getTransactionToReservedBalance())
        .events(aggregate.getDomainEvents())
        .transactions(aggregate.getTransactions()
            .entrySet()
            .stream()
            .map(
                uuidToTransaction -> MoneyTransactionDTO
                    .builder()
                    .transactionUUID(uuidToTransaction.getValue().getTransactionUUID())
                    .fromAccountUUID(uuidToTransaction.getValue().getFromUUID())
                    .toAccountUUID(uuidToTransaction.getValue().getToUUID())
                    .value(uuidToTransaction.getValue().getValue())
                    .state(uuidToTransaction.getValue().getState())
                    .type(uuidToTransaction.getValue().getType())
                    .lastUpdatedAt(uuidToTransaction.getValue().getLastUpdatedAt())
                    .createdAt(uuidToTransaction.getValue().getCreatedAt())
                    .build()
            )
            .collect(Collectors.toMap(MoneyTransactionDTO::getTransactionUUID,
                moneyTransactionDTO -> moneyTransactionDTO)))
        .createdAt(aggregate.getCreatedAt())
        .lastUpdatedAt(aggregate.getLastUpdatedAt())
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
