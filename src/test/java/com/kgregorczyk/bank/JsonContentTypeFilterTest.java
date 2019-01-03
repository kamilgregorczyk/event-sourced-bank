package com.kgregorczyk.bank;


import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_OK;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

class JsonContentTypeFilterTest extends AbstractSparkTest {

  @Test
  public void postMethodWithNoBodyShouldFail() throws Exception {
    HttpGet request = new HttpGet(SERVER_URL + "/");

    // when
    CloseableHttpResponse response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    assertThat(response.getFirstHeader("Content-Type").toString()).isEqualTo(
        "Content-Type: application/json");
    response.close();
  }
}