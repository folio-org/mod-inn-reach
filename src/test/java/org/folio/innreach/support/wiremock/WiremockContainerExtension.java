package org.folio.innreach.support.wiremock;

import com.github.tomakehurst.wiremock.client.HttpAdminClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class WiremockContainerExtension implements BeforeAllCallback, AfterAllCallback {

  public static final int WM_DOCKER_PORT = 8080;
  public static final String WM_URL_PROPERTY = "wm.url";
  public static final String FOLIO_OKAPI_URL_PROPERTY = "folio.okapi-url";

  private static final DockerImageName WM_IMAGE = DockerImageName.parse("wiremock/wiremock:3.13.2");

  private static Admin adminClient;
  private static WireMock client;

  @SuppressWarnings("resource")
  private static final GenericContainer<?> WM_CONTAINER = new GenericContainer<>(WM_IMAGE)
    .withNetwork(Network.SHARED)
    .withExposedPorts(WM_DOCKER_PORT)
    .withAccessToHost(true)
    .withCommand("--local-response-templating", "--disable-banner")
    .withCopyToContainer(MountableFile.forClasspathResource("wm/__files"), "/home/wiremock/__files")
    .withCopyToContainer(MountableFile.forClasspathResource("wm/mappings"), "/home/wiremock/mappings")
    .withLogConsumer(new Slf4jLogConsumer(log).withSeparateOutputStreams());

  public static Admin getWireMockAdminClient() {
    if (adminClient == null) {
      throw new IllegalStateException("WireMock admin client isn't initialized");
    }
    return adminClient;
  }

  public static WireMock getWireMockClient() {
    if (client == null) {
      throw new IllegalStateException("WireMock client isn't initialized");
    }
    return client;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    runContainer();
    var wmUrl = getUrlForExposedPort();
    System.setProperty(WM_URL_PROPERTY, wmUrl);
    System.setProperty(FOLIO_OKAPI_URL_PROPERTY, wmUrl);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(WM_URL_PROPERTY);
    System.clearProperty(FOLIO_OKAPI_URL_PROPERTY);
  }

  private static void runContainer() {
    if (!WM_CONTAINER.isRunning()) {
      WM_CONTAINER.start();

      var wmUrl = getUrlForExposedPort();
      log.info("WireMock server started [url: {}]", wmUrl);

      adminClient = new HttpAdminClient(
        WM_CONTAINER.getHost(), WM_CONTAINER.getMappedPort(WM_DOCKER_PORT));
      client = new WireMock(adminClient);

      // Configure the static WireMock.stubFor() to use this container
      WireMock.configureFor(client);
    }
  }

  private static String getUrlForExposedPort() {
    return String.format("http://%s:%s",
      WM_CONTAINER.getHost(),
      WM_CONTAINER.getMappedPort(WM_DOCKER_PORT));
  }
}
