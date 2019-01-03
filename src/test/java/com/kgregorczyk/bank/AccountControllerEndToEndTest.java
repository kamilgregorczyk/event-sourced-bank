package com.kgregorczyk.bank;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
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
   * Tests whether getting account works.
   */
  @Test
  public void testGetAccountEndpoint() throws Exception {
    // Creates account

    // given
    HttpPost createAccountRequest = createAccountRequest("Tony Stark");

    // when
    CloseableHttpResponse createAccountResponse = client.execute(createAccountRequest);

    // assert
    JsonNode createAccountJson = assertAccountCreatedResponse(createAccountResponse);
    UUID aggregateUUID = UUID.fromString(createAccountJson.get("data").asText());

    // Verifies whether account was created properly

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
    assertThat(accountAsJson.get("events").size()).isEqualTo(1);
    JsonNode accountCreatedEvent = accountAsJson.get("events").get(0);
    assertThat(accountCreatedEvent.get("aggregateUUID").asText())
        .isEqualTo(aggregateUUID.toString());
    assertThat(accountCreatedEvent.get("fullName").asText()).isEqualTo("Tony Stark");
    assertThat(accountCreatedEvent.get("eventType").asText()).isEqualTo("ACCOUNT_CREATED_EVENT");
  }

  /**
   * Tests whether changing account's name works
   */
  @Test
  public void testChangeNameEndpoint() throws Exception {
    // Creates account

    // given
    HttpPost createAccountRequest = createAccountRequest("Tony Stark");

    // when
    CloseableHttpResponse createAccountResponse = client.execute(createAccountRequest);

    // assert
    JsonNode createAccountJson = assertAccountCreatedResponse(createAccountResponse);
    UUID aggregateUUID = UUID.fromString(createAccountJson.get("data").asText());

    // Changes account's full name

    // given
    HttpPost changeFullNameRequest = new HttpPost(
        SERVER_URL + "/api/account/changeFullName/" + aggregateUUID);
    changeFullNameRequest
        .setEntity(
            new StringEntity(toJson(ChangeFullNameRequest.builder().fullName("Iron Man").build())));

    // when
    CloseableHttpResponse changeFullNameResponse = client.execute(changeFullNameRequest);

    // assert
    assertThat(changeFullNameResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    JsonNode changeFullNameJson = OBJECT_MAPPER.readTree(getResponseBody(changeFullNameResponse));
    assertThat(changeFullNameJson.get("status").asText()).isEqualTo("OK");
    assertThat(changeFullNameJson.get("message").asText()).isEqualTo("Name will be changed");

    // Verifies whether account was modified properly

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
    assertThat(accountAsJson.get("fullName").asText()).isEqualTo("Iron Man");
    assertThat(accountAsJson.get("events").size()).isEqualTo(2);
    JsonNode accountCreatedEvent = accountAsJson.get("events").get(1);
    assertThat(accountCreatedEvent.get("aggregateUUID").asText())
        .isEqualTo(aggregateUUID.toString());
    assertThat(accountCreatedEvent.get("fullName").asText()).isEqualTo("Iron Man");
    assertThat(accountCreatedEvent.get("eventType").asText()).isEqualTo("FULL_NAME_CHANGED_EVENT");
  }

  private JsonNode assertAccountCreatedResponse(CloseableHttpResponse createAccountResponse)
      throws Exception {
    JsonNode createAccountJson = OBJECT_MAPPER.readTree(getResponseBody(createAccountResponse));
    assertThat(createAccountResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    assertThat(createAccountJson.get("status").asText()).isEqualTo("OK");
    assertThat(createAccountJson.get("message").asText()).isEqualTo("Account will be created");
    assertThat(createAccountJson.get("data").asText()).isNotEmpty();
    return createAccountJson;
  }

  private HttpPost createAccountRequest(String fullName) throws Exception {
    HttpPost createAccountRequest = new HttpPost(SERVER_URL + "/api/account/createAccount");
    createAccountRequest.setEntity(new StringEntity(toJson(
        CreateAccountRequest.builder().fullName(fullName).build()
    )));
    return createAccountRequest;
  }
}
