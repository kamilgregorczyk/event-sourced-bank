package com.kgregorczyk.bank.controllers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.kgregorczyk.bank.controllers.dto.APIResponse.Status.ERROR;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.kgregorczyk.bank.aggregates.AccountEventStorage;
import com.kgregorczyk.bank.aggregates.AccountService;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import com.kgregorczyk.bank.controllers.dto.AccountDTO;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import java.math.BigDecimal;
import java.util.UUID;
import spark.Route;

/**
 * AccountDTO Controller.
 *
 * <p>During request only basic field validation is done as endpoints are async. Checking for if
 * all requirements/dependencies are met is done later with events.
 *
 * <p>It's possible to: </p>
 * <ul>
 * <li>GET to fetch a list of accounts on `/api/account/listAccounts`</li>
 * <li>GET to fetch a single account on `/api/account/getAccount`</li>
 * <li>POST to create account on `/api/account/createAccount`</li>
 * <li>POST to Change full name on already existing account on `/api/account/changeFullName/VALID_UUID`</li>
 * <li>POST to transfer money on `/api/account/transferMoney`</li>
 * </ul>
 */
public class AccountController {

  private static final String VALIDATION_ERROR_MESSAGE = "There are validation errors";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final AccountService accountService;
  private final AccountEventStorage eventStorage;

  public AccountController(AccountService accountService,
      AccountEventStorage eventStorage) {
    this.accountService = accountService;
    this.eventStorage = eventStorage;
  }

  private static boolean isUUIDNotValid(String value) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    try {
      UUID.fromString(value);
      return false;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  private static ListMultimap<String, String> validationErrorsMap() {
    return MultimapBuilder.hashKeys().arrayListValues()
        .build();
  }

  private static void validateString(String fieldName, String value,
      ListMultimap<String, String> validationErrors) {
    if (value == null || value.isEmpty()) {
      validationErrors.put(fieldName, "Cannot be empty");
    }
  }

  private static void validateUUID(String fieldName, String value,
      ListMultimap<String, String> validationErrors) {
    if (isUUIDNotValid(value)) {
      validationErrors.put(fieldName, "Is not a valid UUID value");
    }
  }

  /**
   * Handles GET requests on `/api/account/listAccounts`
   *
   * @return A list of {@link AccountDTO} of all registered accounts.
   */
  public Route listAccounts() {
    return (request, response) -> new APIResponse(
        eventStorage.loadAll().stream().map(
            AccountDTO::from).collect(toImmutableList()));
  }

  /**
   * Handles GET requests on `/api/account/getAccount/VALID_UUID`
   *
   * @return {@link AccountDTO} for specified account.
   */
  public Route getAccount() {
    return (request, response) -> {
      // Extracts UUID from path
      UUID aggregateUUID;
      try {
        aggregateUUID = UUID.fromString(request.params(":id"));
      } catch (IllegalArgumentException e) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, "UUID provided in path is not valid");
      }

      // Verifies if requested aggregate exists
      if (eventStorage.exists(aggregateUUID)) {
        // Issues ChangeFullNameCommand
        return new APIResponse(AccountDTO.from(eventStorage.loadByUUID(aggregateUUID)));
      } else {
        response.status(HTTP_NOT_FOUND);
        return new APIResponse(ERROR,
            String.format("Account with ID: %s was not found", aggregateUUID));
      }
    };
  }

  /**
   * Handles POST requests on `/api/account/createAccount`.
   *
   * <p>This endpoint issues {@link  AccountService#asyncCreateAccountCommand}.
   * <p> {@link CreateAccountRequest} is used as a request DTO with {@code fullName} as a required
   * field.
   *
   * @return ACK if command was issued properly, HTTP 400 in case of validation errors.
   */
  public Route createAccount() {
    return (request, response) -> {
      CreateAccountRequest payload = OBJECT_MAPPER
          .readValue(request.body(), CreateAccountRequest.class);
      ListMultimap<String, String> validationErrors = validationErrorsMap();

      // Validates request
      validateString("fullName", payload.getFullName(), validationErrors);

      if (!validationErrors.isEmpty()) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, VALIDATION_ERROR_MESSAGE, validationErrors.asMap());
      }

      // Issues CreateAccountCommand
      UUID aggregateUUID = accountService.asyncCreateAccountCommand(payload.getFullName());
      response.status(HTTP_CREATED);
      return new APIResponse(Status.OK, "Account will be created", aggregateUUID);
    };
  }

  /**
   * Handles POST requests on `/api/account/changeFullName/VALID_UUID`.
   *
   * <p>This endpoint issues {@link  AccountService#asyncChangeFullNameCommand}}.
   * <p> {@link ChangeFullNameRequest} is used as a request DTO with {@code fullName} as a required
   * field.
   *
   * @return ACK if command was issued properly, HTTP 404 when aggregate is not found, HTTP 400 in
   * case of validation errors.
   */
  public Route changeFullName() {
    return (request, response) -> {
      ChangeFullNameRequest payload = OBJECT_MAPPER
          .readValue(request.body(), ChangeFullNameRequest.class);
      ListMultimap<String, String> validationErrors = validationErrorsMap();

      // Validates request
      validateString("fullName", payload.getFullName(), validationErrors);
      validateUUID("uuid", request.params(":id"), validationErrors);

      if (!validationErrors.isEmpty()) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, VALIDATION_ERROR_MESSAGE, validationErrors.asMap());
      }

      UUID aggregateUUID = UUID.fromString(request.params(":id"));

      // Verifies if requested aggregate exists
      if (eventStorage.exists(aggregateUUID)) {

        // Issues ChangeFullNameCommand
        accountService.asyncChangeFullNameCommand(aggregateUUID, payload.getFullName());
        return new APIResponse("Name will be changed");
      } else {
        response.status(HTTP_NOT_FOUND);
        return new APIResponse(ERROR,
            String.format("Account with ID: %s was not found", aggregateUUID));
      }
    };
  }

  /**
   * Handles POST requests on `/api/account/transferMoney`.
   *
   * <p>This endpoint issues {@link  AccountService#asyncTransferMoneyCommand}}.
   * <p> {@link TransferMoneyRequest} is used as a request DTO with these fields:
   *
   * <ul>
   * <li>{@code fromAccountNumber} - Valid UUID of issuer aggregate</li>
   * <li>{@code toAccountNumber} - Valid UUID of receiver aggregate</li>
   * <li>{@code value} - Valid, positive double which represent amount of money to transfer.</li>
   * </ul>
   * </p>
   *
   * @return ACK if command was issued properly, HTTP 404 when aggregate is not found, HTTP 400 in
   * case of validation errors.
   */
  public Route transferMoney() {
    return ((request, response) -> {
      TransferMoneyRequest payload = OBJECT_MAPPER
          .readValue(request.body(), TransferMoneyRequest.class);
      ListMultimap<String, String> validationErrors = validationErrorsMap();

      // Validates request
      validateUUID("fromAccountNumber", payload.getFromAccountNumber(), validationErrors);
      validateUUID("toAccountNumber", payload.getToAccountNumber(), validationErrors);

      if (payload.getFromAccountNumber() != null && payload.getToAccountNumber() != null && payload
          .getFromAccountNumber().equals(payload.getToAccountNumber())) {
        validationErrors
            .put("toAccountNumber", "Is not possible to transfer money to the same account");
      }

      if (payload.getValue() == null || payload.getValue().compareTo(BigDecimal.ZERO) <= 0) {
        validationErrors.put("value", "Must be provided & be greater than 0");
      }

      if (!validationErrors.isEmpty()) {
        response.status(HTTP_BAD_REQUEST);
        return new APIResponse(ERROR, VALIDATION_ERROR_MESSAGE, validationErrors.asMap());
      }

      // Validates existence
      UUID fromUUID = UUID.fromString(payload.getFromAccountNumber());
      UUID toUUID = UUID.fromString(payload.getToAccountNumber());
      if (!eventStorage.exists(fromUUID)) {
        response.status(HTTP_NOT_FOUND);
        return new APIResponse(ERROR,
            String.format("Account with UUID: %s doesn't exist", fromUUID));
      }

      if (!eventStorage.exists(toUUID)) {
        response.status(HTTP_NOT_FOUND);
        return new APIResponse(ERROR,
            String.format("Account with UUID: %s doesn't exist", fromUUID));
      }

      // Issues money transfer
      accountService.asyncTransferMoneyCommand(fromUUID, toUUID, payload.getValue());
      response.status(HTTP_CREATED);
      return new APIResponse("Money will be transferred");

    });
  }
}
