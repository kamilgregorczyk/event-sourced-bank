package com.kgregorczyk.bank;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import java.math.BigDecimal;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerEndToEndTest extends SparkTest {

  private static final Gson GSON = new Gson();
  private static final JsonParser JSON_PARSER = new JsonParser();

  private static String createAndAssertAccount(String fullName) throws Exception {
    // given
    HttpPost createAccountRequest = createAccountRequest(fullName);

    // when
    CloseableHttpResponse response = client.execute(createAccountRequest);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    String createAccountJson = getResponseBodyAndClose(response);
    String aggregateUUID =
        GSON.fromJson(createAccountJson, JsonObject.class).get("data").getAsString();
    assertCreateAccountResponse(createAccountJson, aggregateUUID);
    return aggregateUUID;
  }

  private static HttpPost createAccountRequest(String fullName) throws Exception {
    HttpPost createAccountRequest = new HttpPost(SERVER_URL + "/api/account/createAccount");
    createAccountRequest.setEntity(new StringEntity(toJson(
        CreateAccountRequest.builder().fullName(fullName).build()
    )));
    return createAccountRequest;
  }

  private static void assertCreateAccountResponse(String createAccountJson, String aggregateUUID) {
    String expectedResponse = new JSONObject()
        .put("status", "OK")
        .put("message", "Account will be created")
        .put("data", aggregateUUID).toString();
    assertThat(JSON_PARSER.parse(expectedResponse)).isEqualTo(GSON.fromJson(createAccountJson,
        JsonObject.class));
  }

  private static String getAccount(String aggregateUUID) throws Exception {
    // given
    HttpGet getAccountRequest = new HttpGet(
        SERVER_URL + "/api/account/getAccount/" + aggregateUUID);

    // when
    CloseableHttpResponse response = client.execute(getAccountRequest);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    return getResponseBodyAndClose(response);
  }

  private static String getFieldFromEvents(String json, int index, String fieldName) {
    return GSON.fromJson(json, JsonObject.class).getAsJsonObject("data").getAsJsonArray(
        "events").get(index).getAsJsonObject().get(fieldName).getAsString();
  }

  /**
   * Tests whether getting account works.
   */
  @Test
  public void testGetAccountEndpoint() throws Exception {
    // given
    String aggregateUUID = createAndAssertAccount("Tony Stark");

    // when
    String getAccountJson = getAccount(aggregateUUID);

    // assert
    String createdAt = getFieldFromEvents(getAccountJson, 0, "createdAt");
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

    assertThat(JSON_PARSER.parse(expectedResponse))
        .isEqualTo(GSON.fromJson(getAccountJson, JsonObject.class));
  }

  @Test
  public void testChangeFullNameEndpoint() throws Exception {
    // given
    String aggregateUUID = createAndAssertAccount("Tony Stark");
    HttpPost changeFullNameRequest =
        new HttpPost(SERVER_URL + "/api/account/changeFullName/" + aggregateUUID);
    changeFullNameRequest.setEntity(new StringEntity(toJson(
        ChangeFullNameRequest.builder().fullName("Iron Man").build()
    )));

    // when
    CloseableHttpResponse changeFullNameResponse = client.execute(changeFullNameRequest);

    // assert
    assertThat(changeFullNameResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    String changeFullNameJson = getResponseBodyAndClose(changeFullNameResponse);
    String changeFullNameExpectedResponse = new JSONObject()
        .put("status", "OK")
        .put("message", "Full Name will be changed").toString();
    assertThat(JSON_PARSER.parse(changeFullNameExpectedResponse))
        .isEqualTo(GSON.fromJson(changeFullNameJson, JsonObject.class));

    /*
      Verifies if change was properly made by fetching account.
     */

    // when
    String getAccountJson = getAccount(aggregateUUID);

    // assert
    String createdAt = getFieldFromEvents(getAccountJson, 0, "createdAt");
    String lastUpdatedAt = getFieldFromEvents(getAccountJson, 1, "createdAt");
    String expectedResponse = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Iron Man")
            .put("accountNumber", aggregateUUID)
            .put("balance", 1000.0)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray()
                    .put(new JSONObject()
                        .put("fullName", "Tony Stark")
                        .put("eventType", "ACCOUNT_CREATED_EVENT")
                        .put("aggregateUUID", aggregateUUID)
                        .put("createdAt", createdAt))
                    .put(new JSONObject()
                        .put("fullName", "Iron Man")
                        .put("eventType", "FULL_NAME_CHANGED_EVENT")
                        .put("aggregateUUID", aggregateUUID)
                        .put("createdAt", lastUpdatedAt))
            )
            .put("createdAt", createdAt)
            .put("lastUpdatedAt", lastUpdatedAt)
            .put("transactions", new JSONObject())
        ).toString();
    assertThat(JSON_PARSER.parse(expectedResponse))
        .isEqualTo(GSON.fromJson(getAccountJson, JsonObject.class));
  }

  @Test
  public void testMoneyTransferEndpointWhenIssuerHasEnoughMoney() throws Exception {
    // given
    String aggregateUUID1 = createAndAssertAccount("Tony Stark");
    String aggregateUUID2 = createAndAssertAccount("Black Widow");
    transferMoney(aggregateUUID1, aggregateUUID2, 25.01);

    /*
      Verifies Issuer's account
     */

    // when
    String getAccountJson1 = getAccount(aggregateUUID1);

    // assert
    String createdAt1 = getFieldFromEvents(getAccountJson1, 0, "createdAt");
    String lastUpdatedAt1 = getFieldFromEvents(getAccountJson1, 3, "createdAt");
    String expectedResponse1 = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Tony Stark")
            .put("accountNumber", aggregateUUID1)
            .put("balance", 974.99)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray()
                    .put(new JSONObject()
                        .put("eventType", "ACCOUNT_CREATED_EVENT")
                        .put("fullName", "Tony Stark")
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", createdAt1))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFERRED_EVENT")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson1, 1,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 1,
                            "createdAt")))
                    .put(new JSONObject()
                        .put("eventType", "ACCOUNT_DEBITED_EVENT")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson1, 2,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 2,
                            "createdAt")))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFER_SUCCEEDED")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson1, 3,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 3,
                            "createdAt")))
            )
            .put("createdAt", createdAt1)
            .put("lastUpdatedAt", lastUpdatedAt1)
            .put("transactions",
                new JSONObject().put(getFieldFromEvents(getAccountJson1, 1, "transactionUUID"),
                    new JSONObject()
                        .put("transactionUUID",
                            getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                        .put("fromAccountUUID", aggregateUUID1)
                        .put("toAccountUUID", aggregateUUID2)
                        .put("value", -25.01)
                        .put("state", "SUCCEEDED")
                        .put("type", "OUTGOING")
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 1, "createdAt"))
                        .put("lastUpdatedAt", getFieldFromEvents(getAccountJson1, 3, "createdAt"))
                ))

        ).toString();
    assertThat(JSON_PARSER.parse(expectedResponse1)).isEqualTo(GSON.fromJson(getAccountJson1,
        JsonObject.class));

    /*
      Verifies Receiver's account
     */

    // when
    String getAccountJson2 = getAccount(aggregateUUID2);

    // assert
    String createdAt2 = getFieldFromEvents(getAccountJson2, 0, "createdAt");
    String lastUpdatedAt2 = getFieldFromEvents(getAccountJson2, 3, "createdAt");
    String expectedResponse2 = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Black Widow")
            .put("accountNumber", aggregateUUID2)
            .put("balance", 1025.01)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray()
                    .put(new JSONObject()
                        .put("eventType", "ACCOUNT_CREATED_EVENT")
                        .put("fullName", "Black Widow")
                        .put("aggregateUUID", aggregateUUID2)
                        .put("createdAt", createdAt2))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFERRED_EVENT")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson2, 1,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID2)
                        .put("createdAt", getFieldFromEvents(getAccountJson2, 1,
                            "createdAt")))
                    .put(new JSONObject()
                        .put("eventType", "ACCOUNT_CREDITED_EVENT")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson2, 2,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID2)
                        .put("createdAt", getFieldFromEvents(getAccountJson2, 2,
                            "createdAt")))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFER_SUCCEEDED")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson2, 3,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("aggregateUUID", aggregateUUID2)
                        .put("createdAt", getFieldFromEvents(getAccountJson2, 3,
                            "createdAt")))
            )
            .put("createdAt", createdAt2)
            .put("lastUpdatedAt", lastUpdatedAt2)
            .put("transactions",
                new JSONObject().put(getFieldFromEvents(getAccountJson2, 1, "transactionUUID"),
                    new JSONObject()
                        .put("transactionUUID",
                            getFieldFromEvents(getAccountJson2, 1, "transactionUUID"))
                        .put("fromAccountUUID", aggregateUUID1)
                        .put("toAccountUUID", aggregateUUID2)
                        .put("value", 25.01)
                        .put("state", "SUCCEEDED")
                        .put("type", "INCOMING")
                        .put("createdAt", getFieldFromEvents(getAccountJson2, 1, "createdAt"))
                        .put("lastUpdatedAt", getFieldFromEvents(getAccountJson2, 3, "createdAt"))
                ))

        ).toString();
    assertThat(JSON_PARSER.parse(expectedResponse2)).isEqualTo(GSON.fromJson(getAccountJson2,
        JsonObject.class));
  }

  @Test
  public void testMoneyTransferEndpointWhenIssuerDoesNotHaveEnoughMoney() throws Exception {
    // given
    String aggregateUUID1 = createAndAssertAccount("Tony Stark");
    String aggregateUUID2 = createAndAssertAccount("Black Widow");
    transferMoney(aggregateUUID1, aggregateUUID2, 2600.01);

    /*
      Verifies Issuer's account
     */

    // when
    String getAccountJson1 = getAccount(aggregateUUID1);

    // assert
    String createdAt1 = getFieldFromEvents(getAccountJson1, 0, "createdAt");
    String lastUpdatedAt1 = getFieldFromEvents(getAccountJson1, 2, "createdAt");
    String expectedResponse1 = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Tony Stark")
            .put("accountNumber", aggregateUUID1)
            .put("balance", 1000)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray()
                    .put(new JSONObject()
                        .put("eventType", "ACCOUNT_CREATED_EVENT")
                        .put("fullName", "Tony Stark")
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", createdAt1))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFERRED_EVENT")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson1, 1,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 2600.01)
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 1,
                            "createdAt")))
                    .put(new JSONObject()
                        .put("eventType", "MONEY_TRANSFER_CANCELLED")
                        .put("transactionUUID", getFieldFromEvents(getAccountJson1, 2,
                            "transactionUUID"))
                        .put("fromUUID", aggregateUUID1)
                        .put("toUUID", aggregateUUID2)
                        .put("value", 2600.01)
                        .put("reason", "BALANCE_TOO_LOW")
                        .put("aggregateUUID", aggregateUUID1)
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 2,
                            "createdAt")))
            )
            .put("createdAt", createdAt1)
            .put("lastUpdatedAt", lastUpdatedAt1)
            .put("transactions",
                new JSONObject().put(getFieldFromEvents(getAccountJson1, 1, "transactionUUID"),
                    new JSONObject()
                        .put("transactionUUID",
                            getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                        .put("fromAccountUUID", aggregateUUID1)
                        .put("toAccountUUID", aggregateUUID2)
                        .put("value", -2600.01)
                        .put("state", "CANCELLED")
                        .put("type", "OUTGOING")
                        .put("createdAt", getFieldFromEvents(getAccountJson1, 1, "createdAt"))
                        .put("lastUpdatedAt", getFieldFromEvents(getAccountJson1, 2, "createdAt"))
                ))

        ).toString();
    assertThat(JSON_PARSER.parse(expectedResponse1)).isEqualTo(GSON.fromJson(getAccountJson1,
        JsonObject.class));

    /*
      Verifies Receiver's account
     */

    // when
    String getAccountJson2 = getAccount(aggregateUUID2);

    // assert
    String createdAt2 = getFieldFromEvents(getAccountJson1, 0, "createdAt");
    String expectedResponse2 = new JSONObject()
        .put("status", "OK")
        .put("message", "SUCCESS")
        .put("data", new JSONObject()
            .put("fullName", "Black Widow")
            .put("accountNumber", aggregateUUID2)
            .put("balance", 1000.0)
            .put("transactionToReservedBalance", new JSONObject())
            .put("events",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("fullName", "Black Widow")
                            .put("eventType", "ACCOUNT_CREATED_EVENT")
                            .put("aggregateUUID", aggregateUUID2)
                            .put("createdAt", createdAt2)
                    )
            )
            .put("createdAt", createdAt2)
            .put("lastUpdatedAt", createdAt2)
            .put("transactions", new JSONObject())
        ).toString();
    assertThat(JSON_PARSER.parse(expectedResponse2)).isEqualTo(GSON.fromJson(getAccountJson2,
        JsonObject.class));


  }

  private void transferMoney(String aggregateUUID1, String aggregateUUID2, double value)
      throws Exception {
    HttpPost transferMoneyRequest = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    transferMoneyRequest.setEntity(new StringEntity(toJson(
        TransferMoneyRequest.builder().fromAccountNumber(aggregateUUID1)
            .toAccountNumber(aggregateUUID2).value(BigDecimal.valueOf(value)).build()
    )));

    // when
    CloseableHttpResponse transferMoneyResponse = client.execute(transferMoneyRequest);

    // assert
    String transferMoneyExpectedResponse = new JSONObject()
        .put("status", "OK")
        .put("message", "Money will be transferred").toString();
    assertThat(JSON_PARSER.parse(transferMoneyExpectedResponse))
        .isEqualTo(GSON.fromJson(getResponseBodyAndClose(transferMoneyResponse), JsonObject.class));
  }


}
