package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.external.service.InnReachExternalService;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
@ExtendWith(MockitoExtension.class)
class InnReachTransactionControllerIT extends BaseApiControllerTest {

  private static final String CHECK_OUT_ITEM_HOLD_ENDPOINT =
    "/inn-reach/transactions/{itemBarcode}/check-out-item/{servicePointId}";

  private static final String PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE = "DEF-def-5678";
  private static final String CHECK_OUT_BY_BARCODE_URL = "/circulation/check-out-by-barcode";
  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();

  @MockitoBean
  private InnReachExternalService innReachExternalService;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    wm.resetAll();
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkOutItemHoldItem_success() {
    // Stub the check-out-by-barcode endpoint that CirculationClient.checkOutByBarcode calls
    stubPost(CHECK_OUT_BY_BARCODE_URL, "circulation/checkout-response.json");

    // Stub the INN-Reach external call made by the notifier (reportItemShipped)
    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    var result = mockMvc.perform(post(CHECK_OUT_ITEM_HOLD_ENDPOINT,
        PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.folioCheckOut").exists())
      .andExpect(jsonPath("$.folioCheckOut.id").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
      .andExpect(jsonPath("$.folioCheckOut.item.barcode").value(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE))
      .andExpect(jsonPath("$.transaction").exists())
      .andExpect(jsonPath("$.transaction.state").value("ITEM_SHIPPED"))
      .andReturn();

    var response = objectMapper.readValue(
      result.getResponse().getContentAsString(), TransactionCheckOutResponseDTO.class);

    assertNotNull(response.getTransaction());
    assertNotNull(response.getFolioCheckOut());
    assertEquals(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, response.getFolioCheckOut().getItem().getBarcode());
  }
}

