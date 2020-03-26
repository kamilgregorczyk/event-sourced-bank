package com.kgregorczyk.bank.controllers.dto;

import static com.kgregorczyk.bank.controllers.dto.Link.getLinksForAccount;

import com.kgregorczyk.bank.aggregates.AccountAggregate;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.Type;
import com.kgregorczyk.bank.aggregates.events.DomainEvent;
import java.math.BigDecimal;
import java.util.Collection;
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
public class AccountResponse {

  private String fullName;
  private UUID accountNumber;
  private BigDecimal balance;
  private Map<UUID, BigDecimal> transactionToReservedBalance;
  private Collection<DomainEvent> events;
  private Map<UUID, MoneyTransactionDTO> transactions;
  private Date createdAt;
  private Date lastUpdatedAt;
  private List<Link> links;

  public static AccountResponse from(AccountAggregate aggregate) {
    return AccountResponse.builder()
        .accountNumber(aggregate.getUuid())
        .fullName(aggregate.getFullName())
        .balance(aggregate.getBalance())
        .transactionToReservedBalance(aggregate.getTransactionToReservedBalance())
        .events(aggregate.getDomainEvents())
        .transactions(
            aggregate.getTransactions().values().stream()
                .map(
                    moneyTransaction -> MoneyTransactionDTO.builder()
                        .transactionUUID(moneyTransaction.getTransactionUUID())
                        .fromAccountUUID(moneyTransaction.getFromUUID())
                        .toAccountUUID(moneyTransaction.getToUUID())
                        .value(moneyTransaction.getValue())
                        .state(moneyTransaction.getState())
                        .type(moneyTransaction.getType())
                        .lastUpdatedAt(moneyTransaction.getLastUpdatedAt())
                        .createdAt(moneyTransaction.getCreatedAt())
                        .build())
                .collect(
                    Collectors.toMap(
                        MoneyTransactionDTO::getTransactionUUID,
                        moneyTransactionDTO -> moneyTransactionDTO)))
        .createdAt(aggregate.getCreatedAt())
        .lastUpdatedAt(aggregate.getLastUpdatedAt())
        .links(getLinksForAccount(aggregate.getUuid()))
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
