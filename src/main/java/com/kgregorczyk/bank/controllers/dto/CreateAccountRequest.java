package com.kgregorczyk.bank.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateAccountRequest {

  private String fullName;
}
