package com.kgregorczyk.bank;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

public class AccountControllerEndToEndTest extends AbstractEndToEndTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Tests whether creating & getting account works.
   */
  @Test()
  public void testCreateAccount() throws Exception {
    // Creates account

    // given
    HttpPost createAccountRequest = createAccountRequest("Tony Stark");

    // when
    CloseableHttpResponse createAccountResponse = client.execute(createAccountRequest);

    // assert
    JsonNode createAccountJson = OBJECT_MAPPER.readTree(getResponseBody(createAccountResponse));
    assertThat(createAccountResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    assertThat(createAccountJson.get("status").asText()).isEqualTo("OK");
    assertThat(createAccountJson.get("message").asText()).isEqualTo("Account will be created");
    assertThat(createAccountJson.get("data").asText()).isNotEmpty();

    UUID aggregateUUID = UUID.fromString(createAccountJson.get("data").asText());

    // Verifies if account is created properly by calling /api/account/getAccount/aggregateUUID

    // given
    HttpGet getAccountRequest = new HttpGet(
        SERVER_URL + "/api/account/getAccount/" + aggregateUUID);

    // when
    CloseableHttpResponse getAccountResponse = client.execute(getAccountRequest);

    // assert
    assertThat(getAccountResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    JsonNode getAccountJson = OBJECT_MAPPER.readTree(getResponseBody(getAccountResponse));
    assertThat(getAccountJson.get("status").asText()).isEqualTo("OK");
    JsonNode accountAsJson = getAccountJson.get("data");
    assertThat(accountAsJson.get("fullName").asText()).isEqualTo("Tony Stark");
    assertThat(accountAsJson.get("accountNumber").asText()).isEqualTo(aggregateUUID.toString());
    assertThat(accountAsJson.get("balance").asDouble()).isEqualTo(1000.0);
    assertThat(((ArrayNode) accountAsJson.get("events")).size()).isEqualTo(1);
    JsonNode accountCreatedEvent = ((ArrayNode) accountAsJson.get("events")).get(0);
    assertThat(accountCreatedEvent.get("aggregateUUID").asText())
        .isEqualTo(aggregateUUID.toString());
    assertThat(accountCreatedEvent.get("fullName").asText()).isEqualTo("Tony Stark");
    assertThat(accountCreatedEvent.get("eventType").asText()).isEqualTo("ACCOUNT_CREATED_EVENT");


  }

  private HttpPost createAccountRequest(String fullName) throws Exception {
    HttpPost createAccountRequest = new HttpPost(SERVER_URL + "/api/account/createAccount");
    createAccountRequest.setEntity(new StringEntity(toJson(
        CreateAccountRequest.builder().fullName(fullName).build()
    )));
    return createAccountRequest;
  }
}
