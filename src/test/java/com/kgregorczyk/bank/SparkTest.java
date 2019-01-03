package com.kgregorczyk.bank;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import spark.Spark;
import spark.utils.IOUtils;

public abstract class SparkTest {

  protected static final String SERVER_URL = "http://localhost:8000";
  protected static final CloseableHttpClient client = HttpClients.custom().build();
  private static boolean isRunning = false;

  @BeforeAll
  public synchronized static void setUp() {
    if (!isRunning) {
      BankServer.main(null);
      isRunning = true;
    }
  }

  @AfterAll
  public static void tearDown() {
    Spark.awaitStop();
  }


  protected static String getResponseBodyAndClose(CloseableHttpResponse response)
      throws IOException {
    String value = IOUtils.toString(response.getEntity().getContent());
    response.close();
    return value;
  }
}
