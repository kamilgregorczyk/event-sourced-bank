package com.kgregorczyk.bank;

import com.google.common.eventbus.EventBus;
import com.kgregorczyk.bank.aggregates.AccountEventStorage;
import com.kgregorczyk.bank.aggregates.AccountService;
import com.kgregorczyk.bank.aggregates.EventManager;
import com.kgregorczyk.bank.controllers.AccountController;
import com.kgregorczyk.bank.controllers.IndexController;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import com.kgregorczyk.bank.cron.TransactionRollbackCron;
import com.kgregorczyk.bank.filters.CORSFilter;
import com.kgregorczyk.bank.filters.JsonBodyFilter;
import com.kgregorczyk.bank.filters.JsonContentTypeFilter;
import com.kgregorczyk.bank.filters.LoggingFilter;
import com.kgregorczyk.bank.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static spark.Spark.*;

/**
 * Runs HTTP server on port 8000;
 *
 * <p>This service allows to create and list and get given account, change account's name and
 * transfer money from one account to another.
 */
@Slf4j
@SuppressWarnings("FutureReturnValueIgnored")
public class BankServer {

  // TODO: Replace these containers with proper DI tool like Guice
  public static final EventBus EVENT_BUS = new EventBus();
  public static final AccountEventStorage ACCOUNT_EVENT_STORAGE = new AccountEventStorage();
  public static final EventManager EVENT_MANAGER =
      new EventManager(EVENT_BUS, ACCOUNT_EVENT_STORAGE);
  public static final AccountService ACCOUNT_SERVICE = new AccountService(EVENT_BUS);
  public static final AccountController ACCOUNT_CONTROLLER =
      new AccountController(ACCOUNT_SERVICE, ACCOUNT_EVENT_STORAGE);
  public static final TransactionRollbackCron TRANSACTION_ROLLBACK_CRON =
      new TransactionRollbackCron(ACCOUNT_SERVICE, ACCOUNT_EVENT_STORAGE);
  public static final ScheduledExecutorService cronExecutorService =
      Executors.newScheduledThreadPool(1);

  private static final int PORT = 8000;

  public static void main(String[] args) {
    port(PORT);

    // Registers event listener to EventBus
    EVENT_BUS.register(EVENT_MANAGER);

    // Schedules TransactionRollbackCron

    cronExecutorService.scheduleAtFixedRate(TRANSACTION_ROLLBACK_CRON, 0, 5, TimeUnit.MINUTES);

    // Before filter
    before(new LoggingFilter());
    before("/api/*", new JsonBodyFilter());

    // After filters
    afterAfter(new JsonContentTypeFilter());
    afterAfter(new CORSFilter());

    // Controllers
    path(
        "",
        () -> {
          get("/", IndexController.healthCheck(), JsonUtils::toJson);
          path(
              "/api",
              () ->
                  path(
                      "/account",
                      () -> {
                        get("", ACCOUNT_CONTROLLER.listAccounts(), JsonUtils::toJson);
                        post("", ACCOUNT_CONTROLLER.createAccount(), JsonUtils::toJson);
                        get("/:id", ACCOUNT_CONTROLLER.getAccount(), JsonUtils::toJson);
                        put(
                            "/:id/changeFullName",
                            ACCOUNT_CONTROLLER.changeFullName(),
                            JsonUtils::toJson);
                        post(
                            "/transferMoney",
                            ACCOUNT_CONTROLLER.transferMoney(),
                            JsonUtils::toJson);
                      }));
        });

    // Other handlers
    notFound(
        (request, response) ->
            new APIResponse(Status.ERROR, "Requested resource doesn't exist").toJson());
    internalServerError(
        (request, response) -> new APIResponse(Status.ERROR, "Internal Server Error").toJson());

    awaitInitialization();
    logMessage();
  }

  private static void logMessage() {
    log.info("***************************************");
    log.info("*** Bank server is running on :{} ***", PORT);
    log.info("***************************************");
  }
}
