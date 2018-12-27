package com.kgregorczyk.bank.aggregates.events;

import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public abstract class DomainEvent {

  private UUID aggregateUUID;
  private Date createdAt;

}
