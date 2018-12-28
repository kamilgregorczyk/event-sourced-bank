package com.kgregorczyk.bank.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferMoneyRequest {

  private String fromAccountNumber;
  private String toAccountNumber;
  private BigDecimal value;
}
