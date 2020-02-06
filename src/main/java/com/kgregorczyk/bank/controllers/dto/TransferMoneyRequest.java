package com.kgregorczyk.bank.controllers.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMoneyRequest {

  private String fromAccountNumber;
  private String toAccountNumber;
  private BigDecimal value;
}
