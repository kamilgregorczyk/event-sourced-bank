package com.kgregorczyk.bank;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import spark.Spark;
import spark.utils.IOUtils;

public abstract class AbstractEndToEndTest {

  static final String SERVER_URL = "http://localhost:8000";
  static final CloseableHttpClient client = HttpClients.custom().build();
  private static boolean isRunning = false;

  @BeforeAll
  public static void setUp() {
    if (!isRunning) {
      BankServer.main(null);
      isRunning = true;
    }
  }

  @AfterAll
  public static void tearDown() {
    Spark.awaitStop();
  }


  static String getResponseBody(CloseableHttpResponse response) throws IOException {
    return IOUtils.toString(response.getEntity().getContent());
  }
}
