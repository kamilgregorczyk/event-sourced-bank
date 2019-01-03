package com.kgregorczyk.bank.controllers;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerTransferMoneyTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static CloseableHttpResponse createAccount() throws Exception {
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity(toJson(new CreateAccountRequest("Kamil Gregorczyk"))));
    return client.execute(request);
  }

  private static String extractUUIDFromResponseAndClose(CloseableHttpResponse response)
      throws Exception {
    return (String) GSON.fromJson(getResponseBodyAndClose(response), APIResponse.class).getData();

  }

  @Test
  public void transferMoneyValid() throws Exception {
    // given
    String aggregateUUID1 = extractUUIDFromResponseAndClose(createAccount());
    String aggregateUUID2 = extractUUIDFromResponseAndClose(createAccount());
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest(aggregateUUID1,
        aggregateUUID2, BigDecimal.TEN))));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    String expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Money will be transferred")
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNegativeValue() throws Exception {
    // given
    String aggregateUUID1 = extractUUIDFromResponseAndClose(createAccount());
    String aggregateUUID2 = extractUUIDFromResponseAndClose(createAccount());
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest(aggregateUUID1,
        aggregateUUID2, BigDecimal.valueOf(-10)))));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("value", new JSONArray().put("Must be provided & "
                + "be greater than 0")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNoBody() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest())));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject()
                .put("fromAccountNumber", new JSONArray()
                    .put("Is not a valid UUID value")
                )
                .put("toAccountNumber", new JSONArray()
                    .put("Is not a valid UUID value")
                ).put("value", new JSONArray()
                    .put("Must be provided & be greater than 0")
                )
            )
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidSameAccountNumbers() throws Exception {
    // given
    String aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest(aggregateUUID,
        aggregateUUID, BigDecimal.TEN))));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject()
                .put("toAccountNumber", new JSONArray()
                    .put("Is not possible to transfer money to the same account")
                )
            )
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNotExistingFromAccount() throws Exception {
    // given
    String randomUUID = UUID.randomUUID().toString();
    String aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(toJson(new TransferMoneyRequest(randomUUID,
        aggregateUUID, BigDecimal.TEN))));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", String.format("Account with UUID: %s doesn't exist", randomUUID))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void transferMoneyNotValidNotExistingToAccount() throws Exception {
    // given
    String randomUUID = UUID.randomUUID().toString();
    String aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    request.setEntity(new StringEntity(
        toJson(new TransferMoneyRequest(aggregateUUID, randomUUID, BigDecimal.TEN))));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", String.format("Account with UUID: %s doesn't exist", randomUUID))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }
}