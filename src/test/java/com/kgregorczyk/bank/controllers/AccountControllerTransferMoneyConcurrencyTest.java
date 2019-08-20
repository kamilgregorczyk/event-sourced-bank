package com.kgregorczyk.bank.controllers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.BankServer.ACCOUNT_EVENT_STORAGE;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

public class AccountControllerTransferMoneyConcurrencyTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static CloseableHttpResponse createAccount() throws Exception {
    var request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity(toJson(new CreateAccountRequest("Tony Stark"))));
    return client.execute(request);
  }

  private static String extractUUIDFromResponseAndClose(CloseableHttpResponse response)
      throws Exception {
    return (String) GSON.fromJson(getResponseBodyAndClose(response), APIResponse.class).getData();
  }

  @Test
  public void transferMoneyValid() throws Exception {
    // given
    var aggregateUUID1 = extractUUIDFromResponseAndClose(createAccount());
    var aggregateUUID2 = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(
            toJson(new TransferMoneyRequest(aggregateUUID1, aggregateUUID2, BigDecimal.ONE))));
    var threadPool = Executors.newCachedThreadPool();

    // when
    var futures = IntStream.range(1, 501).boxed()
        .map(i -> threadPool.submit(() -> {
          try {
            client.execute(request).close();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        })).collect(toImmutableList());

    futures.forEach(future -> {
      try {
        future.get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    });
    // assert
    assertThat(ACCOUNT_EVENT_STORAGE.get(UUID.fromString(aggregateUUID1)).getBalance()
        .compareTo(BigDecimal.valueOf(500))).isEqualTo(0);
    assertThat(ACCOUNT_EVENT_STORAGE.get(UUID.fromString(aggregateUUID2)).getBalance()
        .compareTo(BigDecimal.valueOf(1500))).isEqualTo(0);
  }
}
