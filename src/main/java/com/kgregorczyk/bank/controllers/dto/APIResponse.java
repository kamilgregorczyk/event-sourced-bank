package com.kgregorczyk.bank.controllers.dto;

import com.kgregorczyk.bank.utils.JsonUtils;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Main DTO which is used as a response to every REST request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APIResponse {

  private Status status;
  private String message;
  private Object data;
  private List<Link> _links;

  public APIResponse(String message) {
    this(Status.OK, message, null, null);
  }

  public APIResponse(Status status, String message) {
    this(status, message, null, null);
  }

  public APIResponse(Status status, String message, Object data) {
    this(status, message, data, null);
  }

  public APIResponse(Object data, List<Link> links) {
    this(Status.OK, "SUCCESS", data, links);
  }

  public APIResponse(String message, List<Link> links) {
    this(Status.OK, message, null, links);
  }

  public String toJson() {
    return JsonUtils.toJson(this);
  }

  @AllArgsConstructor
  public enum Status {
    OK("OK"),
    ERROR("ERROR");

    private final String value;
  }
}
