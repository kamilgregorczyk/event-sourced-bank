package com.kgregorczyk.bank.cron;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;

import com.google.common.collect.ListMultimap;
import com.kgregorczyk.bank.aggregates.AccountEventStorage;
import com.kgregorczyk.bank.aggregates.AccountService;
import com.kgregorczyk.bank.aggregates.MoneyTransaction;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.State;
import com.kgregorczyk.bank.aggregates.MoneyTransaction.Type;
import com.kgregorczyk.bank.aggregates.events.MoneyTransferCancelled.Reason;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Finds unfinished transactions with where untouched for more than {@link
 * TransactionRollbackCron#TRANSACTION_TIMEOUT_IN_MINUTES} and cancels them.
 *
 * <p>Runs every 5 minutes, right after startup.
 */
@Slf4j
public class TransactionRollbackCron implements Runnable {

  private static final int TRANSACTION_TIMEOUT_IN_MINUTES = 30;

  private final AccountService accountService;
  private final AccountEventStorage eventStorage;

  public TransactionRollbackCron(AccountService accountService, AccountEventStorage eventStorage) {
    this.accountService = accountService;
    this.eventStorage = eventStorage;
  }

  private static MoneyTransaction findTransaction(List<MoneyTransaction> transactions, Type type) {
    if (transactions.get(0).getType().equals(type)) {
      return transactions.get(0);
    }
    return transactions.get(1);
  }

  private static boolean bothTransactionsAreInState(MoneyTransaction issuerTransaction,
      MoneyTransaction receiverTransaction, State state) {
    return issuerTransaction.getState().equals(state) && receiverTransaction.getState()
        .equals(state);
  }

  @Override
  public void run() {
    log.info("TransactionRollbackCron has started");
    Date thresholdDate = Date
        .from(Instant.now().minus(TRANSACTION_TIMEOUT_IN_MINUTES, ChronoUnit.MINUTES));

    // Transactions have to be grouped by it's UUID as there might be two unfinished transactions
    // of the same money transfer or one finished and 2nd one not.
    ListMultimap<UUID, MoneyTransaction> outDatedTransactions = findOutDatedTransactions(
        thresholdDate);

    for (Map.Entry<UUID, Collection<MoneyTransaction>> uuidToTransactions : outDatedTransactions
        .asMap().entrySet()) {
      List<MoneyTransaction> transactions = new ArrayList<>(uuidToTransactions.getValue());

      // If there is one transaction then the event was only received by issuer's account
      if (transactions.size() == 1) {
        rollbackSingleTransaction(transactions.get(0));
      } else {
        rollbackTwoTransactions(findTransaction(transactions, Type.OUTGOING),
            findTransaction(transactions, Type.INCOMING));
      }
    }
    log.info("TransactionRollbackCron has finished");
  }

  private ListMultimap<UUID, MoneyTransaction> findOutDatedTransactions(
      Date thresholdDate) {
    return eventStorage
        .loadAll().stream()
        .flatMap(accountAggregate -> accountAggregate.getTransactions().entrySet().stream())
        .map(Map.Entry::getValue)
        .filter(transaction -> transaction.getLastUpdatedAt().before(thresholdDate))
        .collect(toImmutableListMultimap(MoneyTransaction::getTransactionUUID,
            moneyTransaction -> moneyTransaction));
  }

  private void rollbackSingleTransaction(MoneyTransaction transaction) {
    // Already cancelled transaction doesn't need to be cancelled again
    if (!transaction.getState().equals(State.CANCELLED)) {
      cancelTransactionForIssuer(transaction);
    }
  }

  private void rollbackTwoTransactions(MoneyTransaction issuerTransaction,
      MoneyTransaction receiverTransaction) {
    // We filter out transactions with two entries with state SUCCEEDED or CANCELLED
    if (!bothTransactionsAreInState(issuerTransaction, receiverTransaction, State.SUCCEEDED)
        && !bothTransactionsAreInState(issuerTransaction, receiverTransaction,
        State.CANCELLED)) {

      // Otherwise we have two transactions where one is not finished.
      if (!issuerTransaction.getState().equals(State.CANCELLED)) {
        cancelTransactionForIssuer(issuerTransaction);
      }
      if (!receiverTransaction.getState().equals(State.CANCELLED)) {
        cancelTransactionForReceiver(receiverTransaction);
      }
    }
  }

  private void cancelTransactionForIssuer(MoneyTransaction transaction) {
    log.info("Cancelling transaction: {}", transaction);
    cancelTransaction(transaction.getFromUUID(), transaction);
  }

  private void cancelTransactionForReceiver(MoneyTransaction transaction) {
    log.info("Cancelling transaction: {}", transaction);
    cancelTransaction(transaction.getToUUID(), transaction);
  }

  private void cancelTransaction(UUID aggregateUUID, MoneyTransaction transaction) {
    accountService.asyncCancelTransactionCommand(aggregateUUID,
        transaction.getFromUUID(), transaction.getToUUID(),
        transaction.getTransactionUUID(), transaction.getValue().abs(),
        Reason.INTERNAL_SERVER_ERROR);
  }
}