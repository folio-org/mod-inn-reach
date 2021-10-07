package org.folio.innreach.controller.d2ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.domain.CirculationOperation.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createCirculationRequestDTO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

class InnReachCirculationControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "fli01";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private InnReachTransactionRepository repository;

  @Test
  void processCreatePatronHoldCirculationRequest_and_createNewPatronHold() {
    var circulationRequestDTO = createCirculationRequestDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      circulationRequestDTO, InnReachResponseDTO.class, PATRON_HOLD.getOperationName(), "tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNull(responseBody.getErrors());
    assertEquals(InnReachResponseStatus.OK.getResponseStatus(), responseBody.getStatus());

    var innReachTransaction = repository.findByTrackingIdAndAndCentralServerCode("tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertTrue(innReachTransaction.isPresent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCreatePatronHoldCirculationRequest_and_updateExitingPatronHold() {
    var circulationRequestDTO = createCirculationRequestDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      circulationRequestDTO, InnReachResponseDTO.class, PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNull(responseBody.getErrors());
    assertEquals(InnReachResponseStatus.OK.getResponseStatus(), responseBody.getStatus());
  }
}
