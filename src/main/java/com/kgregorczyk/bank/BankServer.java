package com.kgregorczyk.bank;

import static com.kgregorczyk.bank.Singletons.EVENT_MANAGER;
import static spark.Spark.afterAfter;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;

import com.kgregorczyk.bank.aggregates.AccountAggregate;
import com.kgregorczyk.bank.controllers.AccountController;
import com.kgregorczyk.bank.controllers.IndexController;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import com.kgregorczyk.bank.filters.JsonBodyFilter;
import com.kgregorczyk.bank.filters.JsonContentTypeFilter;
import com.kgregorczyk.bank.filters.LoggingFilter;
import com.kgregorczyk.bank.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs HTTP server on port 8000;
 */
@Slf4j
public class BankServer {

  private static final int PORT = 8000;

  public static void main(String[] args) {
    port(PORT);

    // Registers event listener to EventBus
    Singletons.EVENT_BUS.register(EVENT_MANAGER);

    // Before filter
    before(new LoggingFilter());
    before("/api/*", new JsonBodyFilter());

    // Controllers
    path("", () -> {
      get("/", IndexController.healthCheck());
      path("/api", () -> path("/account", () -> {
        get("/getAccount/:id", AccountController.getAccount(), JsonUtils::toJson);
        get("/listAccounts", AccountController.listAccounts(), JsonUtils::toJson);
        post("/createAccount", AccountController.createAccount(), JsonUtils::toJson);
        post("/changeFullName", AccountController.changeFullName(), JsonUtils::toJson);
        post("/transferMoney", AccountController.transferMoney(), JsonUtils::toJson);
      }));
    });

    // After filters
    afterAfter("/api/*", new JsonContentTypeFilter());

    // Other handlers
    notFound((request, response) ->
        new APIResponse(Status.ERROR, "Requested resource doesn't exist").toJson());
    internalServerError(
        (request, response) -> new APIResponse(Status.ERROR, "Internal Server Error").toJson());

    awaitInitialization();
    logMessage();

    AccountAggregate.createAccountCommand("Kamil Gregorczyk");
    AccountAggregate.createAccountCommand("Noemi Gregorczyk");
  }

  private static void logMessage() {
    log.info("***************************************");
    log.info("*** Bank server is running on :{} ***", PORT);
    log.info("***************************************");
  }
}
