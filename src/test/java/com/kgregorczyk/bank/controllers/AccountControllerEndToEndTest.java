package com.kgregorczyk.bank.controllers;

import static com.google.common.truth.Truth.assertThat;
import static com.kgregorczyk.bank.utils.JsonUtils.toJson;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.google.gson.JsonObject;
import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.ChangeFullNameRequest;
import com.kgregorczyk.bank.controllers.dto.CreateAccountRequest;
import com.kgregorczyk.bank.controllers.dto.Link;
import com.kgregorczyk.bank.controllers.dto.TransferMoneyRequest;
import java.math.BigDecimal;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class AccountControllerEndToEndTest extends AbstractSparkTest {

  private static String createAndAssertAccount(String fullName) throws Exception {
    // given
    var createAccountRequest = createAccountRequest(fullName);

    // when
    var response = client.execute(createAccountRequest);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_CREATED);
    var createAccountJson = getResponseBodyAndClose(response);
    var aggregateUUID =
        GSON.fromJson(createAccountJson, JsonObject.class).get("data").getAsString();
    assertCreateAccountResponse(createAccountJson, aggregateUUID);
    return aggregateUUID;
  }

  private static HttpPost createAccountRequest(String fullName) throws Exception {
    var createAccountRequest = new HttpPost(SERVER_URL + "/api/account");
    createAccountRequest.setEntity(
        new StringEntity(toJson(CreateAccountRequest.builder().fullName(fullName).build())));
    return createAccountRequest;
  }

  private static void assertCreateAccountResponse(String createAccountJson, String aggregateUUID) {
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Account will be created")
            .put("data", aggregateUUID)
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .toString();
    assertResponses(expectedResponse, createAccountJson);
  }

  private static String getAccount(String aggregateUUID) throws Exception {
    // given
    var getAccountRequest = new HttpGet(SERVER_URL + "/api/account/" + aggregateUUID);

    // when
    var response = client.execute(getAccountRequest);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    return getResponseBodyAndClose(response);
  }

  /** Tests whether getting account works. */
  @Test
  public void testGetAccountEndpoint() throws Exception {
    // given
    var aggregateUUID = createAndAssertAccount("Tony Stark");

    // when
    var getAccountJson = getAccount(aggregateUUID);

    // assert
    var createdAt = getFieldFromEvents(getAccountJson, 0, "createdAt");
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .put(
                "data",
                new JSONObject()
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
                    .put("links", Link.getLinksForAccount(aggregateUUID))
                    .put("transactions", new JSONObject()))
            .toString();
    assertResponses(expectedResponse, getAccountJson);
  }

  @Test
  public void testChangeFullNameEndpoint() throws Exception {
    // given
    var aggregateUUID = createAndAssertAccount("Tony Stark");
    var changeFullNameRequest =
        new HttpPut(SERVER_URL + "/api/account/" + aggregateUUID + "/changeFullName");
    changeFullNameRequest.setEntity(
        new StringEntity(toJson(ChangeFullNameRequest.builder().fullName("Iron Man").build())));

    // when
    var changeFullNameResponse = client.execute(changeFullNameRequest);

    // assert
    assertThat(changeFullNameResponse.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    var changeFullNameJson = getResponseBodyAndClose(changeFullNameResponse);
    var changeFullNameExpectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Full Name will be changed")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .toString();
    assertResponses(changeFullNameExpectedResponse, changeFullNameJson);

    /*
    Verifies if change was properly made by fetching account.
    */

    // when
    var getAccountJson = getAccount(aggregateUUID);

    // assert
    var createdAt = getFieldFromEvents(getAccountJson, 0, "createdAt");
    var lastUpdatedAt = getFieldFromEvents(getAccountJson, 1, "createdAt");
    var expectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID))
            .put(
                "data",
                new JSONObject()
                    .put("fullName", "Iron Man")
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
                                    .put("createdAt", createdAt))
                            .put(
                                new JSONObject()
                                    .put("fullName", "Iron Man")
                                    .put("eventType", "FULL_NAME_CHANGED_EVENT")
                                    .put("aggregateUUID", aggregateUUID)
                                    .put("createdAt", lastUpdatedAt)))
                    .put("createdAt", createdAt)
                    .put("lastUpdatedAt", lastUpdatedAt)
                    .put("transactions", new JSONObject())
                    .put("links", Link.getLinksForAccount(aggregateUUID)))
            .toString();
    assertResponses(expectedResponse, getAccountJson);
  }

  @Test
  public void testMoneyTransferEndpointWhenIssuerHasEnoughMoney() throws Exception {
    // given
    var aggregateUUID1 = createAndAssertAccount("Tony Stark");
    var aggregateUUID2 = createAndAssertAccount("Black Widow");
    transferMoney(aggregateUUID1, aggregateUUID2, 25.01);

    /*
    Verifies Issuer's account
    */

    // when
    var getAccountJson1 = getAccount(aggregateUUID1);

    // assert
    var createdAt1 = getFieldFromEvents(getAccountJson1, 0, "createdAt");
    var lastUpdatedAt1 = getFieldFromEvents(getAccountJson1, 3, "createdAt");
    var expectedResponse1 =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID1))
            .put(
                "data",
                new JSONObject()
                    .put("fullName", "Tony Stark")
                    .put("accountNumber", aggregateUUID1)
                    .put("balance", 974.99)
                    .put("transactionToReservedBalance", new JSONObject())
                    .put("links", Link.getLinksForAccount(aggregateUUID1))
                    .put(
                        "events",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("eventType", "ACCOUNT_CREATED_EVENT")
                                    .put("fullName", "Tony Stark")
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put("createdAt", createdAt1))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFERRED_EVENT")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 1, "createdAt")))
                            .put(
                                new JSONObject()
                                    .put("eventType", "ACCOUNT_DEBITED_EVENT")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 2, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 2, "createdAt")))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFER_SUCCEEDED")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 3, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 3, "createdAt"))))
                    .put("createdAt", createdAt1)
                    .put("lastUpdatedAt", lastUpdatedAt1)
                    .put(
                        "transactions",
                        new JSONObject()
                            .put(
                                getFieldFromEvents(getAccountJson1, 1, "transactionUUID"),
                                new JSONObject()
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                                    .put("fromAccountUUID", aggregateUUID1)
                                    .put("toAccountUUID", aggregateUUID2)
                                    .put("value", -25.01)
                                    .put("state", "SUCCEEDED")
                                    .put("type", "OUTGOING")
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 1, "createdAt"))
                                    .put(
                                        "lastUpdatedAt",
                                        getFieldFromEvents(getAccountJson1, 3, "createdAt")))))
            .toString();
    assertResponses(expectedResponse1, getAccountJson1);

    /*
    Verifies Receiver's account
    */

    // when
    var getAccountJson2 = getAccount(aggregateUUID2);

    // assert
    var createdAt2 = getFieldFromEvents(getAccountJson2, 0, "createdAt");
    var lastUpdatedAt2 = getFieldFromEvents(getAccountJson2, 3, "createdAt");
    var expectedResponse2 =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID2))
            .put(
                "data",
                new JSONObject()
                    .put("fullName", "Black Widow")
                    .put("accountNumber", aggregateUUID2)
                    .put("balance", 1025.01)
                    .put("transactionToReservedBalance", new JSONObject())
                    .put(
                        "events",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("eventType", "ACCOUNT_CREATED_EVENT")
                                    .put("fullName", "Black Widow")
                                    .put("aggregateUUID", aggregateUUID2)
                                    .put("createdAt", createdAt2))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFERRED_EVENT")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson2, 1, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID2)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson2, 1, "createdAt")))
                            .put(
                                new JSONObject()
                                    .put("eventType", "ACCOUNT_CREDITED_EVENT")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson2, 2, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID2)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson2, 2, "createdAt")))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFER_SUCCEEDED")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson2, 3, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("aggregateUUID", aggregateUUID2)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson2, 3, "createdAt"))))
                    .put("createdAt", createdAt2)
                    .put("lastUpdatedAt", lastUpdatedAt2)
                    .put("links", Link.getLinksForAccount(aggregateUUID2))
                    .put(
                        "transactions",
                        new JSONObject()
                            .put(
                                getFieldFromEvents(getAccountJson2, 1, "transactionUUID"),
                                new JSONObject()
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson2, 1, "transactionUUID"))
                                    .put("fromAccountUUID", aggregateUUID1)
                                    .put("toAccountUUID", aggregateUUID2)
                                    .put("value", 25.01)
                                    .put("state", "SUCCEEDED")
                                    .put("type", "INCOMING")
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson2, 1, "createdAt"))
                                    .put(
                                        "lastUpdatedAt",
                                        getFieldFromEvents(getAccountJson2, 3, "createdAt")))))
            .toString();
    assertResponses(expectedResponse2, getAccountJson2);
  }

  @Test
  public void testMoneyTransferEndpointWhenIssuerDoesNotHaveEnoughMoney() throws Exception {
    // given
    var aggregateUUID1 = createAndAssertAccount("Tony Stark");
    var aggregateUUID2 = createAndAssertAccount("Black Widow");
    transferMoney(aggregateUUID1, aggregateUUID2, 2600.01);

    /*
    Verifies Issuer's account
    */

    // when
    var getAccountJson1 = getAccount(aggregateUUID1);

    // assert
    var createdAt1 = getFieldFromEvents(getAccountJson1, 0, "createdAt");
    var lastUpdatedAt1 = getFieldFromEvents(getAccountJson1, 2, "createdAt");
    var expectedResponse1 =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID1))
            .put(
                "data",
                new JSONObject()
                    .put("fullName", "Tony Stark")
                    .put("accountNumber", aggregateUUID1)
                    .put("balance", 1000)
                    .put("transactionToReservedBalance", new JSONObject())
                    .put("links", Link.getLinksForAccount(aggregateUUID1))
                    .put(
                        "events",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("eventType", "ACCOUNT_CREATED_EVENT")
                                    .put("fullName", "Tony Stark")
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put("createdAt", createdAt1))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFERRED_EVENT")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 2600.01)
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 1, "createdAt")))
                            .put(
                                new JSONObject()
                                    .put("eventType", "MONEY_TRANSFER_CANCELLED")
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 2, "transactionUUID"))
                                    .put("fromUUID", aggregateUUID1)
                                    .put("toUUID", aggregateUUID2)
                                    .put("value", 2600.01)
                                    .put("reason", "BALANCE_TOO_LOW")
                                    .put("aggregateUUID", aggregateUUID1)
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 2, "createdAt"))))
                    .put("createdAt", createdAt1)
                    .put("lastUpdatedAt", lastUpdatedAt1)
                    .put(
                        "transactions",
                        new JSONObject()
                            .put(
                                getFieldFromEvents(getAccountJson1, 1, "transactionUUID"),
                                new JSONObject()
                                    .put(
                                        "transactionUUID",
                                        getFieldFromEvents(getAccountJson1, 1, "transactionUUID"))
                                    .put("fromAccountUUID", aggregateUUID1)
                                    .put("toAccountUUID", aggregateUUID2)
                                    .put("value", -2600.01)
                                    .put("state", "CANCELLED")
                                    .put("type", "OUTGOING")
                                    .put(
                                        "createdAt",
                                        getFieldFromEvents(getAccountJson1, 1, "createdAt"))
                                    .put(
                                        "lastUpdatedAt",
                                        getFieldFromEvents(getAccountJson1, 2, "createdAt")))))
            .toString();
    assertResponses(expectedResponse1, getAccountJson1);

    /*
    Verifies Receiver's account
    */

    // when
    var getAccountJson2 = getAccount(aggregateUUID2);

    // assert
    var createdAt2 = getFieldFromEvents(getAccountJson2, 0, "createdAt");
    var expectedResponse2 =
        new JSONObject()
            .put("status", "OK")
            .put("message", "SUCCESS")
            .put("links", Link.getLinksForAccount(aggregateUUID2))
            .put(
                "data",
                new JSONObject()
                    .put("links", Link.getLinksForAccount(aggregateUUID2))
                    .put("fullName", "Black Widow")
                    .put("accountNumber", aggregateUUID2)
                    .put("balance", 1000.0)
                    .put("transactionToReservedBalance", new JSONObject())
                    .put(
                        "events",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("fullName", "Black Widow")
                                    .put("eventType", "ACCOUNT_CREATED_EVENT")
                                    .put("aggregateUUID", aggregateUUID2)
                                    .put("createdAt", createdAt2)))
                    .put("createdAt", createdAt2)
                    .put("lastUpdatedAt", createdAt2)
                    .put("transactions", new JSONObject()))
            .toString();
    assertResponses(expectedResponse2, getAccountJson2);
  }

  private void transferMoney(String aggregateUUID1, String aggregateUUID2, double value)
      throws Exception {
    var transferMoneyRequest = new HttpPost(SERVER_URL + "/api/account/transferMoney");
    transferMoneyRequest.setEntity(
        new StringEntity(
            toJson(
                TransferMoneyRequest.builder()
                    .fromAccountNumber(aggregateUUID1)
                    .toAccountNumber(aggregateUUID2)
                    .value(BigDecimal.valueOf(value))
                    .build())));

    // when
    var transferMoneyResponse = client.execute(transferMoneyRequest);

    // assert
    var transferMoneyExpectedResponse =
        new JSONObject()
            .put("status", "OK")
            .put("message", "Money will be transferred")
            .put("links", Link.getLinksForAccounts())
            .toString();
    assertResponses(transferMoneyExpectedResponse, getResponseBodyAndClose(transferMoneyResponse));
  }
}
