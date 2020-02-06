package com.kgregorczyk.bank.controllers;

import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_OK;

import com.kgregorczyk.bank.AbstractSparkTest;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.utils.JsonUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

public class IndexControllerEndToEndTest extends AbstractSparkTest {

  @Test
  public void testIndexController() throws Exception {
    // given
    var request = new HttpGet(SERVER_URL + "/");

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    assertThat(getResponseBodyAndClose(response))
        .isEqualTo(JsonUtils.toJson(new APIResponse("System is OK")));
  }
}
