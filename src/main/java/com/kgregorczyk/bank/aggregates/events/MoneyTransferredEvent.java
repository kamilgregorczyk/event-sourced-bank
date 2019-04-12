package com.kgregorczyk.bank.aggregates.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MoneyTransferredEvent extends DomainEvent {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;

  public MoneyTransferredEvent(
      UUID aggregateUUID, UUID fromUUID, UUID toUUID, UUID transactionUUID, BigDecimal value) {
    this(aggregateUUID, fromUUID, toUUID, transactionUUID, value, new Date());
  }

  public MoneyTransferredEvent(
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
