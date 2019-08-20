package com.kgregorczyk.bank.controllers;

import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.Link;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.BankServer.ACCOUNT_EVENT_STORAGE;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

public class AccountControllerTransferMoneyTest extends AbstractSparkTest {

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
            toJson(new TransferMoneyRequest(aggregateUUID1, aggregateUUID2, BigDecimal.TEN))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Money will be transferred")
            .put("links", Link.getLinksForAccounts())
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
    assertThat(ACCOUNT_EVENT_STORAGE.get(UUID.fromString(aggregateUUID1)).getBalance().compareTo(BigDecimal.valueOf(990))).isEqualTo(0);
    assertThat(ACCOUNT_EVENT_STORAGE.get(UUID.fromString(aggregateUUID2)).getBalance().compareTo(BigDecimal.valueOf(1010))).isEqualTo(0);
  }

  @Test
  public void transferMoneyNotValidNegativeValue() throws Exception {
    // given
    var aggregateUUID1 = extractUUIDFromResponseAndClose(createAccount());
    var aggregateUUID2 = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(
            toJson(
                new TransferMoneyRequest(
                    aggregateUUID1, aggregateUUID2, BigDecimal.valueOf(-10)))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put(
                "data",
                new JSONObject()
                    .put("value", new JSONArray().put("Must be provided & " + "be greater than 0")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNoBody() throws Exception {
    // given
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest())));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put(
                "data",
                new JSONObject()
                    .put("fromAccountNumber", new JSONArray().put("Is not a valid UUID value"))
                    .put("toAccountNumber", new JSONArray().put("Is not a valid UUID value"))
                    .put("value", new JSONArray().put("Must be provided & be greater than 0")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidSameAccountNumbers() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(
            toJson(new TransferMoneyRequest(aggregateUUID, aggregateUUID, BigDecimal.TEN))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put(
                "data",
                new JSONObject()
                    .put(
                        "toAccountNumber",
                        new JSONArray()
                            .put("Is not possible to transfer money to the same account")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidAccountNumbersAreNulls() throws Exception {
    // given
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(toJson(new TransferMoneyRequest(null, null, BigDecimal.TEN))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put(
                "data",
                new JSONObject()
                    .put("fromAccountNumber", new JSONArray().put("Is not a valid UUID value"))
                    .put("toAccountNumber", new JSONArray().put("Is not a valid UUID value")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNotExistingFromAccount() throws Exception {
    // given
    var randomUUID = UUID.randomUUID().toString();
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(
            toJson(new TransferMoneyRequest(randomUUID, aggregateUUID, BigDecimal.TEN))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", String.format("Account with UUID: %s doesn't exist", randomUUID))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNotExistingToAccount() throws Exception {
    // given
    var randomUUID = UUID.randomUUID().toString();
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(
        new StringEntity(
            toJson(new TransferMoneyRequest(aggregateUUID, randomUUID, BigDecimal.TEN))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", String.format("Account with UUID: %s doesn't exist", randomUUID))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }
}
