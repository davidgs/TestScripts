package io.camunda.testscript;

import io.camunda.zeebe.client.ZeebeClient;

public class ZeebeClientFactory {

  private static final String ZEEBE_ADDRESS = "";
  private static final String ZEEBE_CLIENT_ID = "";
  private static final String ZEEBE_CLIENT_SECRET = "";
  private static final String ZEEBE_REGION = "bru-2";

  public static ZeebeClient getZeebeClient() {
    return ZeebeClient.newCloudClientBuilder()
        .withClusterId(ZEEBE_ADDRESS)
        .withClientId(ZEEBE_CLIENT_ID)
        .withClientSecret(ZEEBE_CLIENT_SECRET)
        .withRegion(ZEEBE_REGION)
        .build();
  }

}
