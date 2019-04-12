package com.kgregorczyk.bank.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_OK;

public class AccountControllerListAccountsTest extends AbstractSparkTest {

  private static final Gson GSON = new Gson();

  private static CloseableHttpResponse createAccount() throws Exception {
    HttpPost request = new HttpPost(SERVER_URL + "/api/account");
    request.setEntity(new StringEntity(toJson(new CreateAccountRequest("Tony Stark"))));
    return client.execute(request);
  }

  @Test
  public void listAccounts() throws Exception {
    // given
    CloseableHttpResponse response1 = createAccount();
    CloseableHttpResponse response2 = createAccount();
    response1.close();
    response2.close();
    HttpGet request = new HttpGet(SERVER_URL + "/api/account");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    String responseJson = getResponseBodyAndClose(response);
    assertThat(GSON.fromJson(responseJson, JsonObject.class).get("data").getAsJsonArray().size())
        .isGreaterThan(1);
  }
}
