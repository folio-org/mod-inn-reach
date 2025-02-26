package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.external.service.InnReachExternalService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InnReachProxyControllerTest extends BaseControllerTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @MockitoBean
  private InnReachExternalService innReachExternalService;

  @ParameterizedTest
  @MethodSource("innReachUriList")
  void should_handleAllRequestsWithD2RSuffixInUrl(String innReachUri) {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/edab6baf-c696-42b1-89bb-1bbb8759b0d2/d2r" + innReachUri, String.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());

    verify(innReachExternalService).callInnReachApi(UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2"), innReachUri);
  }

  private static List<String> innReachUriList() {
    return List.of(
      "/contribution/itemtypes",
      "/circ/patrontypes",
      "/contribution/locations",
      "/contribution/localservers");
  }
}
