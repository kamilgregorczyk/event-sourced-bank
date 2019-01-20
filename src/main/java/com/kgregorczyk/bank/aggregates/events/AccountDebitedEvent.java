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
public class AccountDebitedEvent extends DomainEvent {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;

  public AccountDebitedEvent(
      UUID aggregateUUID, UUID fromUUID, UUID toUUID, UUID transactionUUID, BigDecimal value) {
    super(aggregateUUID, new Date());
    this.transactionUUID = transactionUUID;
    this.fromUUID = fromUUID;
    this.toUUID = toUUID;
    this.value = value;
  }
}
