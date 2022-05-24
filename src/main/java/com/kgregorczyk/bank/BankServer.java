package com.kgregorczyk.bank;

import static java.lang.Integer.parseInt;
import static spark.Spark.afterAfter;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

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
import com.kgregorczyk.bank.filters.StartDateApplyingFilter;
import com.kgregorczyk.bank.utils.JsonUtils;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

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

  public static void main(String[] args) {
    final var port = args.length == 1 ? parseInt(args[0]) : 8000;
    port(port);

    // Registers event listener to EventBus
    EVENT_BUS.register(EVENT_MANAGER);

    // Schedules TransactionRollbackCron
    cronExecutorService.scheduleAtFixedRate(TRANSACTION_ROLLBACK_CRON, 0, 5, TimeUnit.MINUTES);

    // Before filter
    before(new StartDateApplyingFilter());
    before("/api/*", new JsonBodyFilter());

    // After filters
    afterAfter(new JsonContentTypeFilter());
    afterAfter(new CORSFilter());
    afterAfter(new LoggingFilter());

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
    createDummyAccounts();
    logMessage(port);
  }

  private static void createDummyAccounts() {
    final var accountId1 = ACCOUNT_SERVICE.asyncCreateAccountCommand("Kamil Gregorczyk");
    final var accountId2 = ACCOUNT_SERVICE.asyncCreateAccountCommand("John Doe");
    final var accountId3 = ACCOUNT_SERVICE.asyncCreateAccountCommand("Piotr Kowalski");
    ACCOUNT_SERVICE.asyncTransferMoneyCommand(accountId1, accountId2, BigDecimal.TEN);
    ACCOUNT_SERVICE.asyncTransferMoneyCommand(accountId1, accountId2, BigDecimal.valueOf(100_000));
    ACCOUNT_SERVICE.asyncChangeFullNameCommand(accountId3, "Jan Kowalski");
  }

  private static void logMessage(int port) {
    log.info("***************************************");
    log.info("*** Bank server is running on :{} ***", port);
    log.info("***************************************");
  }
}
