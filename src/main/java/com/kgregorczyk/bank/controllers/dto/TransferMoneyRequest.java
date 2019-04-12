package com.kgregorczyk.bank.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMoneyRequest {

  private String fromAccountNumber;
  private String toAccountNumber;
  private BigDecimal value;
}
