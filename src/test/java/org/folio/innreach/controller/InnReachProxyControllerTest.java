package org.folio.innreach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.external.service.InnReachExternalService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InnReachProxyControllerTest extends BaseControllerTest {

  private static final String CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @LocalServerPort
  private int port;

  @MockitoBean
  private InnReachExternalService innReachExternalService;

  @ParameterizedTest
  @MethodSource("innReachUriList")
  void should_handleAllRequestsWithD2RSuffixInUrl(String innReachUri) {
    var url = String.format("/inn-reach/central-servers/%s/d2r%s", CENTRAL_SERVER_ID, innReachUri);

    var responseEntity = testRestTemplate.getForEntity(url, String.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());

    verify(innReachExternalService).callInnReachApi(UUID.fromString(CENTRAL_SERVER_ID), innReachUri);
  }

  @Test
  void should_returnReadableJsonExactlyAsReceivedFromInnReach() throws java.io.IOException, InterruptedException {
    String serverJson = """
        {
                "status": "ok",
                "reason": "success",
                "errors": [],
                "itemTypeList": [
                  {
                      "centralItemType": 200,
                      "description": "IR Book"
                  },
                  {
                      "centralItemType": 201,
                      "description": "Digital Media"
                  }
             ]
        }""";

    when(innReachExternalService.callInnReachApi(any(UUID.class), any(String.class))).thenReturn(serverJson);

    // Use the raw JDK HttpClient to fetch the body as bytes without any JSON decoding.
    var httpClient = HttpClient.newHttpClient();
    var httpRequest = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:" + port
        + "/inn-reach/central-servers/edab6baf-c696-42b1-89bb-1bbb8759b0d2/d2r/contribution/itemtypes?limit=1000"))
      .GET()
      .build();

    HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

    assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    assertThat(MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
    // Body must be the exact readable JSON the central server sent (no base64, no extra quoting,
    // no Jackson re-indentation).
    assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo(serverJson);
  }

  private static List<String> innReachUriList() {
    return List.of(
      "/contribution/itemtypes",
      "/circ/patrontypes",
      "/contribution/locations",
      "/contribution/localservers");
  }
}
