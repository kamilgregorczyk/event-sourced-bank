package com.kgregorczyk.bank.aggregates;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class MoneyTransaction {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;
  private State state;

  public enum State {
    NEW, PENDING, SUCCEEDED, CANCELLED
  }
}
