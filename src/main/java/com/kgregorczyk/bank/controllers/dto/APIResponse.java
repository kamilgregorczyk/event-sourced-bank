package com.kgregorczyk.bank.controllers.dto;

import com.kgregorczyk.bank.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Main DTO which is used as a response to every REST request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APIResponse {

  private Status status;
  private String message;
  private Object data;

  public APIResponse(Object data) {
    this(Status.OK, "SUCCESS", data);
  }

  public APIResponse(String message) {
    this(Status.OK, message, null);
  }

  public APIResponse(Status status, String message) {
    this(status, message, null);
  }

  public String toJson() {
    return JsonUtils.toJson(this);
  }


  @AllArgsConstructor
  public enum Status {
    OK("OK"),
    ERROR("ERROR");

    private String value;

  }
}
