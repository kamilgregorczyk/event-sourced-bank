package com.kgregorczyk.bank.aggregates.events;

import java.math.BigDecimal;
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
public class MoneyTransferSucceeded extends DomainEvent {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;

  public MoneyTransferSucceeded(
      UUID aggregateUUID, UUID fromUUID, UUID toUUID, UUID transactionUUID, BigDecimal value) {
    super(aggregateUUID, new Date());
    this.transactionUUID = transactionUUID;
    this.fromUUID = fromUUID;
    this.toUUID = toUUID;
    this.value = value;
  }

  public MoneyTransferSucceeded(
      UUID aggregateUUID,
      UUID fromUUID,
      UUID toUUID,
      UUID transactionUUID,
      BigDecimal value,
      Date date) {
    super(aggregateUUID, date);
    this.transactionUUID = transactionUUID;
    this.fromUUID = fromUUID;
    this.toUUID = toUUID;
    this.value = value;
  }
}
