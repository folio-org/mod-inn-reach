package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.external.dto.InnReachResponse.ERROR_STATUS;
import static org.folio.innreach.external.dto.InnReachResponse.OK_STATUS;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.BibInfoResponseDTO;

@Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql", executionPhase = AFTER_TEST_METHOD)
@SqlMergeMode(MERGE)
class BibInfoControllerTest extends BaseApiControllerTest {

  private static final String BIB_ID = "bib001313";
  private static final String CENTRAL_CODE = "d2ir";
  private static final String GET_BIB_RECORD_URL = "/inn-reach/d2ir/getbibrecord/{bibId}/{centralCode}";
  private static final String INSTANCE_BY_HRID_URL_TEMPLATE = "/inventory-view/instances?query=(instanceHrid==%s)&limit=1";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @MockitoBean
  private RecordTransformationService recordTransformationService;

  private final HttpHeaders headers = getOkapiHeaders();

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void shouldReturnBibInfo_whenInstanceFoundByHrid() {
    var bibInfo = new BibInfo();
    bibInfo.setBibId(BIB_ID);
    bibInfo.setItemCount(1);
    when(recordTransformationService.getBibInfo(any(UUID.class), any())).thenReturn(bibInfo);

    stubGet(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID), "inventory-view/instance-view-response.json");

    var responseEntity = testRestTemplate.postForEntity(GET_BIB_RECORD_URL,
      new HttpEntity<>(headers), BibInfoResponseDTO.class, BIB_ID, CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var response = responseEntity.getBody();
    assertNotNull(response);
    assertEquals(OK_STATUS, response.getStatus());
    assertNotNull(response.getBibInfo());
    assertEquals(BIB_ID, response.getBibInfo().getBibId());

    verify(getRequestedFor(urlEqualTo(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID))));
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void shouldReturnErrorResponse_whenInstanceNotFoundByHrid() {
    stubGet(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID), "inventory-view/empty-result-response.json");

    var responseEntity = testRestTemplate.postForEntity(GET_BIB_RECORD_URL,
      new HttpEntity<>(headers), BibInfoResponseDTO.class, BIB_ID, CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var response = responseEntity.getBody();
    assertNotNull(response);
    assertEquals(ERROR_STATUS, response.getStatus());

    verify(getRequestedFor(urlEqualTo(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID))));
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void shouldUseInstanceHridTermQuery_notLegacyDottedAttributeQuery() {
    // Stub ONLY the new-style URL; intentionally do not stub the legacy
    // "/inventory-view/instances?query=(instance.hrid=bib001313)&limit=1" shape.
    stubGet(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID), "inventory-view/instance-view-response.json");

    var bibInfo = new BibInfo();
    bibInfo.setBibId(BIB_ID);
    when(recordTransformationService.getBibInfo(any(UUID.class), any())).thenReturn(bibInfo);

    testRestTemplate.postForEntity(GET_BIB_RECORD_URL,
      new HttpEntity<>(headers), BibInfoResponseDTO.class, BIB_ID, CENTRAL_CODE);

    // Asserts the exact on-the-wire query string uses the term-search operator `==`
    // against the flat `instanceHrid` attribute, not the legacy dotted
    // `instance.hrid` attribute with no operator.
    verify(getRequestedFor(urlEqualTo(String.format(INSTANCE_BY_HRID_URL_TEMPLATE, BIB_ID))));
  }

}
