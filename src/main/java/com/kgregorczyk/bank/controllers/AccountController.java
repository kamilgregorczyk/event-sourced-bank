package com.kgregorczyk.bank.controllers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.kgregorczyk.bank.Singletons.ACCOUNT_EVENT_STORAGE;
import static com.kgregorczyk.bank.aggregates.AccountAggregate.changeFullNameCommand;
import static com.kgregorczyk.bank.aggregates.AccountAggregate.createAccountCommand;
import static com.kgregorczyk.bank.controllers.dto.APIResponse.Status.ERROR;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.Account;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import java.util.UUID;
import spark.Route;

/**
 * Account Controller.
 *
 * <p>During request only basic field validation is done as endpoints are async. Checking for if
 * all requirements/dependencies are met is done later with events.
 *
 * <p>It's possible to: </p>
 * <ul>
 * <li>POST to create account on `/api/account/createAccount`</li>
 * <li>GET to fetch a list of accounts on `/api/account/listAccounts`</li>
 * <li>POST to Change full name on already existing account on `/api/account/changeFullName/VALID_UUID`</li>
 * </ul>
 */
public class AccountController {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Route createAccount() {
    return (request, response) -> {
      // Validates request
      CreateAccountRequest payload = OBJECT_MAPPER
          .readValue(request.body(), CreateAccountRequest.class);

      if (payload.getFullName() == null || payload.getFullName().isEmpty()) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, "`fullName` is a required string field");
      }

      // Issues CreateAccountCommand
      createAccountCommand(payload.getFullName());
      return new APIResponse();
    };
  }

  public static Route listAccounts() {
    return (request, response) -> new APIResponse(
        ACCOUNT_EVENT_STORAGE.loadAll().stream().map(
            Account::from).collect(toImmutableList()));
  }

  public static Route changeFullName() {
    return (request, response) -> {
      // Validates request
      ChangeFullNameRequest payload = OBJECT_MAPPER
          .readValue(request.body(), ChangeFullNameRequest.class);

      if (payload.getFullName() == null || payload.getFullName().isEmpty()) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, "`fullName` is a required string field");
      }

      // Extracts UUID from path
      UUID aggregateUUID;
      try {
        aggregateUUID = UUID.fromString(request.params(":id"));
      } catch (IllegalArgumentException e) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, "UUID provided in path is not valid");

      }

      // Verifies if requested aggregate exists
      if (ACCOUNT_EVENT_STORAGE.exists(aggregateUUID)) {
        // Issues ChangeFullNameCommand
        changeFullNameCommand(aggregateUUID, payload.getFullName());
        return new APIResponse();
      } else {
        response.status(HTTP_NOT_FOUND);
        return new APIResponse(ERROR,
            String.format("Account with ID: %s was not found", aggregateUUID));
      }
    };
  }
}
