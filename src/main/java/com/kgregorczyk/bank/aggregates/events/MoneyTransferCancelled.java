package com.kgregorczyk.bank.aggregates.events;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MoneyTransferCancelled extends DomainEvent {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;
  private Reason reason;

  public MoneyTransferCancelled(
      UUID aggregateUUID,
      UUID fromUUID,
      UUID toUUID,
      UUID transactionUUID,
      BigDecimal value,
      Reason reason) {
    this(aggregateUUID, fromUUID, toUUID, transactionUUID, value, reason, new Date());
  }

  public MoneyTransferCancelled(
      UUID aggregateUUID,
      UUID fromUUID,
      UUID toUUID,
      UUID transactionUUID,
      BigDecimal value,
      Reason reason,
      Date date) {
    super(aggregateUUID, date);
    this.transactionUUID = transactionUUID;
    this.fromUUID = fromUUID;
    this.toUUID = toUUID;
    this.value = value;
    this.reason = reason;
  }

  public enum Reason {
    BALANCE_TOO_LOW,
    INTERNAL_SERVER_ERROR
  }
}
