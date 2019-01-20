package com.kgregorczyk.bank.controllers;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerCreateAccountTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static CloseableHttpResponse createAccount() throws Exception {
    HttpPost request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity(toJson(new CreateAccountRequest("Tony Stark"))));
    return client.execute(request);
  }

  @Test
  public void createAccountValid() throws Exception {
    // when
    CloseableHttpResponse response = createAccount();

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    String responseJson = getResponseBodyAndClose(response);
    String aggregateUUID = GSON.fromJson(responseJson, JsonObject.class).get("data").getAsString();
    String expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Account will be created")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .put("data", aggregateUUID)
            .toString();
    assertResponses(expectedResponse, responseJson);
  }

  @Test
  public void createAccountNotValidNoFullName() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity("{}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void createAccountNotValidNullFullName() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity("{\"fullName\": null}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void createAccountNotValidEmptyFullName() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity("{\"fullName\": \"\"}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    String expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }
}
