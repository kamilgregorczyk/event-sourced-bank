package com.kgregorczyk.bank.filters;

import com.kgregorczyk.bank.AbstractSparkTest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static java.net.HttpURLConnection.HTTP_OK;

class JsonContentTypeFilterTest extends AbstractSparkTest {

  @Test
  public void postMethodWithNoBodyShouldFail() throws Exception {
    var request = new HttpGet(SERVER_URL + "/");

    // when
    var response = client.execute(request);

    // assert
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HTTP_OK);
    assertThat(response.getFirstHeader("Content-Type").toString())
        .isEqualTo("Content-Type: application/json");
    response.close();
  }
}
