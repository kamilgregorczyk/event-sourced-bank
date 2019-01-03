package com.kgregorczyk.bank.filters;


import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.google.gson.Gson;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

class JsonBodyFilterTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static void assertResponseForEmptyBody(CloseableHttpResponse response) throws Exception {
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    assertThat(GSON.fromJson(getResponseBodyAndClose(response), APIResponse.class)).isEqualTo(
        new APIResponse(Status.ERROR, "Body is empty"));
  }

  private static void assertResponseForNotValidJson(CloseableHttpResponse response)
      throws Exception {
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
    assertThat(GSON.fromJson(getResponseBodyAndClose(response), APIResponse.class)).isEqualTo(
        new APIResponse(Status.ERROR, "Body is required and it is not in JSON format"));
  }

  @Test
  public void postMethodWithNoBodyShouldFail() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/createAccount");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForEmptyBody(response);
  }

  @Test
  public void putMethodWithNoBodyShouldFail() throws Exception {
    // given
    HttpPut request = new HttpPut(SERVER_URL + "/api/account/createAccount");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForEmptyBody(response);

  }

  @Test
  public void patchMethodWithNoBodyShouldFail() throws Exception {
    // given
    HttpPatch request = new HttpPatch(SERVER_URL + "/api/account/createAccount");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForEmptyBody(response);
  }

  @Test
  public void postMethodWithNotValidJsonShouldFail() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity("{\"a\":}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForNotValidJson(response);
  }

  @Test
  public void putMethodWithNotValidJsonShouldFail() throws Exception {
    // given
    HttpPut request = new HttpPut(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity("{\"a\":}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForNotValidJson(response);
  }

  @Test
  public void patchMethodWithNotValidJsonShouldFail() throws Exception {
    // given
    HttpPatch request = new HttpPatch(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity("{\"a\":}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertResponseForNotValidJson(response);
  }

  @Test
  public void postMethodWithValidBodyShouldNotFail() throws Exception {
    // given
    HttpPost request = new HttpPost(SERVER_URL + "/api/account/createAccount");
    request.setEntity(new StringEntity("{\"fullName\":\"Kamil Gregorczyk\"}"));

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    response.close();
  }

  @Test
  public void getMethodShouldNotFail() throws Exception {
    // given
    HttpGet request = new HttpGet(SERVER_URL + "/");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    response.close();
  }

}