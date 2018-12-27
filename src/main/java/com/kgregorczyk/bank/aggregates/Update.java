package com.kgregorczyk.bank.aggregates;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class Update {

  private String description;
  private Date issuedAt;
}
