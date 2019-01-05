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
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerGetAccountTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static CloseableHttpResponse createAccount() throws Exception {
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity(toJson(new CreateAccountRequest("Tony Stark"))));
    return client.execute(request);
  }

  private static String extractUUIDFromResponseAndClose(CloseableHttpResponse response)
      throws Exception {
    return (String) GSON.fromJson(getResponseBodyAndClose(response), APIResponse.class).getData();

  }


  @Test
  public void getAccountValid() throws Exception {
    // given
    String aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    HttpGet request =
        new HttpGet(SERVER_URL + "/api/account/getAccount/" + aggregateUUID);

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    String responseJson = getResponseBodyAndClose(response);
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    String createdAt = getFieldFromEvents(responseJson, 0, "createdAt");
    String expectedResponse = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Tony Stark")
            .put("accountNumber", aggregateUUID)
            .put("balance", 1000.0)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray().put(new JSONObject().put("fullName", "Tony Stark")
                    .put("eventType", "ACCOUNT_CREATED_EVENT").put("aggregateUUID",
                        aggregateUUID).put("createdAt", createdAt)))
            .put("createdAt", createdAt)
            .put("lastUpdatedAt", createdAt)
            .put("transactions", new JSONObject())
        ).toString();

    assertResponses(expectedResponse, responseJson);

  }

  @Test
  public void getAccountNotValidInvalidUUID() throws Exception {
    // given
    HttpGet request =
        new HttpGet(SERVER_URL + "/api/account/getAccount/asd");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("uuid", new JSONArray()
                .put("Is not a valid UUID value")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void getAccountNotValidAggregateDoesNotExist() throws Exception {
    // given
    UUID aggregateUUID = UUID.randomUUID();
    HttpGet request =
        new HttpGet(SERVER_URL + "/api/account/getAccount/" + aggregateUUID.toString());

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message",
                String.format("Account with ID: %s was not found", aggregateUUID.toString()))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

}
