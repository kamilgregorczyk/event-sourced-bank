package com.kgregorczyk.bank.controllers;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.Link;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerChangeFullNameTest extends AbstractSparkTest {

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
  public void changeFullNameValid() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID + "/changeFullName");
    request.setEntity(new StringEntity(toJson(new ChangeFullNameRequest("Superman"))));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Full Name will be changed")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void changeFullNameNotValidInvalidUUID() throws Exception {
    // given
    var request = new HttpPut(SERVER_URL + "/api/account/asd/changeFullName");
    request.setEntity(new StringEntity(toJson(new ChangeFullNameRequest("Superman"))));

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
  public void changeFullNameNotValidAggregateDoesNotExist() throws Exception {
    // given
    var aggregateUUID = UUID.randomUUID();
    var request =
        new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID.toString() + "/changeFullName");
    request.setEntity(new StringEntity(toJson(new ChangeFullNameRequest("Superman"))));

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

  @Test
  public void changeFullNameNotValidNoFullName() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID + "/changeFullName");
    request.setEntity(new StringEntity("{}"));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void changeFullNameNotValidEmptyFullName() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID + "/changeFullName");
    request.setEntity(new StringEntity("{\"fullName\": \"\"}"));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }

  @Test
  public void changeFullNameNotValidNullFullName() throws Exception {
    // given
    var aggregateUUID = extractUUIDFromResponseAndClose(createAccount());
    var request = new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID + "/changeFullName");
    request.setEntity(new StringEntity("{\"fullName\": null}"));

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    var expectedResponse =
        new JSONObject()
            .put("status", "ERROR")
            .put("message", "There are validation errors")
            .put("data", new JSONObject().put("fullName", new JSONArray().put("Cannot be empty")))
            .toString();
    assertResponses(expectedResponse, getResponseBodyAndClose(response));
  }
}
