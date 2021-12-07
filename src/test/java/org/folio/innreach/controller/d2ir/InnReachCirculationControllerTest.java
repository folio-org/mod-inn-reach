package org.folio.innreach.controller.d2ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachCirculationControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";

  private static final String PATRON_HOLD_OPERATION = "patronhold";
  private static final String ITEM_SHIPPED_OPERATION = "itemshipped";


  @Autowired
  private TestRestTemplate testRestTemplate;

  @SpyBean
  private InnReachTransactionRepository repository;

  @MockBean
  private ItemService itemService;

  @Test
  void processCreatePatronHoldCirculationRequest_and_createNewPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      transactionHoldDTO, InnReachResponseDTO.class, PATRON_HOLD_OPERATION, "tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var innReachTransaction = repository.findByTrackingIdAndCentralServerCode("tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertTrue(innReachTransaction.isPresent());
    assertNotNull(innReachTransaction.get().getHold());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCreatePatronHoldCirculationRequest_and_updateExitingPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      transactionHoldDTO, InnReachResponseDTO.class, PATRON_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var updatedTransaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertTrue(updatedTransaction.isPresent());

    var innReachTransaction = updatedTransaction.get();

    assertEquals(transactionHoldDTO.getTransactionTime(), innReachTransaction.getHold().getTransactionTime());
    assertEquals(transactionHoldDTO.getPatronId(), innReachTransaction.getHold().getPatronId());
    assertEquals(transactionHoldDTO.getPatronAgencyCode(), innReachTransaction.getHold().getPatronAgencyCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemShippedCircRequest_updateFolioItem_whenAssociatedItemExists() {
    when(itemService.getItemByBarcode(any())).thenReturn(InventoryItemDTO.builder().build());
    when(itemService.find(any())).thenReturn(Optional.of(InventoryItemDTO.builder().build()));

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
        "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}", HttpMethod.PUT,
        new HttpEntity<>(transactionHoldDTO), InnReachResponseDTO.class,
        ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    verify(itemService).changeAndUpdate(any(), any());

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseEntityBody = responseEntity.getBody();

    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemShippedCircRequest_doNotUpdateFolioItem_whenAssociatedItemDoesNotExist() {
    when(itemService.getItemByBarcode(any())).thenReturn(InventoryItemDTO.builder().build());
    when(itemService.find(any())).thenReturn(Optional.empty());

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO), InnReachResponseDTO.class,
      ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    verify(itemService, times(0)).update(any());

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseEntityBody = responseEntity.getBody();

    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());
  }

}
