package com.kgregorczyk.bank.controllers;

import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

public class AccountControllerGetAccountTest extends AbstractSparkTest {

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
  public void getAccountValid() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpGet(SERVER_URL + "/api/account/" + aggregateUUID);

    // when
    var response = client.execute(request);

    // assert
    var responseJson = getResponseBodyAndClose(response);
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    var createdAt = getFieldFromEvents(responseJson, 0, "createdAt");
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .put(
                "data",
                new JSONObject()
                    .put("links", Link.getLinksForAccount(aggregateUUID))
                    .put("fullName", "Tony Stark")
                    .put("accountNumber", aggregateUUID)
                    .put("balance", 1000.0)
                    .put("transactionToReservedBalance", new JSONObject())
                    .put(
                        "events",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("fullName", "Tony Stark")
                                    .put("eventType", "ACCOUNT_CREATED_EVENT")
                                    .put("aggregateUUID", aggregateUUID)
                                    .put("createdAt", createdAt)))
                    .put("createdAt", createdAt)
                    .put("lastUpdatedAt", createdAt)
                    .put("transactions", new JSONObject()))
            .toString();

    assertResponses(expectedResponse, responseJson);
  }

  @Test
  public void getAccountNotValidInvalidUUID() throws Exception {
    // given
    var request = new HttpGet(SERVER_URL + "/api/account/asd");

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
                new JSONObject().put("uuid", new JSONArray().put("Is not a valid UUID value")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void getAccountNotValidAggregateDoesNotExist() throws Exception {
    // given
    var aggregateUUID = UUID.randomUUID();
    var request = new HttpGet(SERVER_URL + "/api/account/" + aggregateUUID.toString());

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put(
                "message",
                String.format("Account with ID: %s was not found", aggregateUUID.toString()))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }
}
