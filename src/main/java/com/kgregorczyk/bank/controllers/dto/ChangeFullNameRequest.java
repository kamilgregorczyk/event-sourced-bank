package com.kgregorczyk.bank.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeFullNameRequest {

  private UUID accountNumber;
  private String fullName;
}
