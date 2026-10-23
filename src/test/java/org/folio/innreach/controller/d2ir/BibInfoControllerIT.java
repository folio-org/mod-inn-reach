package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.innreach.external.dto.InnReachResponse.ERROR_STATUS;
import static org.folio.innreach.external.dto.InnReachResponse.OK_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.BibInfoResponseDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.it.base.BaseTenantIT;

class BibInfoControllerIT extends BaseTenantIT {

  private static final String GET_BIB_RECORD_PATH = "/inn-reach/d2ir/getbibrecord/{bibId}/{centralCode}";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String INVENTORY_VIEW_INSTANCES_URL = "/inventory-view/instances";

  @MockitoBean
  private RecordTransformationService recordTransformationService;

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql", executionPhase = AFTER_TEST_METHOD)
  void return200AndBibInfo_and_sendsInstanceHridEqualsQuery_when_instanceFound() throws Exception {
    var bibId = "in00000000355";

    stubGet(INVENTORY_VIEW_INSTANCES_URL, "inventory-view/query-instance-by-hrid-response.json",
      Map.of("query", "(instanceHrid==" + bibId + ")", "limit", "1"));

    var bibInfo = new BibInfo();
    bibInfo.setBibId(bibId);
    when(recordTransformationService.getBibInfo(any(UUID.class), any(Instance.class)))
      .thenReturn(bibInfo);

    var result = mockMvc.perform(post(GET_BIB_RECORD_PATH, bibId, PRE_POPULATED_CENTRAL_CODE)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var response = fromJson(result, BibInfoResponseDTO.class);
    assertEquals(OK_STATUS, response.getStatus());
    assertNotNull(response.getBibInfo());
    assertEquals(bibId, response.getBibInfo().getBibId());

    wiremock.verifyThat(1, getRequestedFor(urlPathEqualTo(INVENTORY_VIEW_INSTANCES_URL))
      .withQueryParam("query", equalTo("(instanceHrid==" + bibId + ")"))
      .withQueryParam("limit", equalTo("1")));
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql", executionPhase = AFTER_TEST_METHOD)
  void return200AndErrorResponse_when_instanceNotFound() throws Exception {
    var bibId = "in00000000999";

    stubGet(INVENTORY_VIEW_INSTANCES_URL, "inventory-view/empty-instance-response.json",
      Map.of("query", "(instanceHrid==" + bibId + ")", "limit", "1"));

    var result = mockMvc.perform(post(GET_BIB_RECORD_PATH, bibId, PRE_POPULATED_CENTRAL_CODE)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var response = fromJson(result, BibInfoResponseDTO.class);
    assertEquals(ERROR_STATUS, response.getStatus());

    wiremock.verifyThat(1, getRequestedFor(urlPathEqualTo(INVENTORY_VIEW_INSTANCES_URL))
      .withQueryParam("query", equalTo("(instanceHrid==" + bibId + ")"))
      .withQueryParam("limit", equalTo("1")));
  }
}
