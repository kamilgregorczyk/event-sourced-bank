package com.kgregorczyk.bank.aggregates;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MoneyTransaction {

  private UUID transactionUUID;
  private UUID fromUUID;
  private UUID toUUID;
  private BigDecimal value;
  private State state;
  private Type type;
  private Date lastUpdatedAt;
  private Date createdAt;

  public enum State {
    NEW,
    PENDING,
    SUCCEEDED,
    CANCELLED
  }

  public enum Type {
    INCOMING,
    OUTGOING
  }
}
