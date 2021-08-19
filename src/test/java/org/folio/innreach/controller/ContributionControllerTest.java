package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationRequest;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.external.client.feign.InventoryStorageClient;
import org.folio.innreach.repository.ContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ContributionControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @MockBean
  private InventoryStorageClient client;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ContributionRepository repository;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return201HttpCode_whenInstanceIterationStarted(){
    doNothing().when(client).startInitialContribution(any(InstanceIterationRequest.class));

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/contributions", HttpEntity.EMPTY, Void.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

    var fromDb = repository.fetchCurrentByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));
    assertNotNull(fromDb);
    assertEquals(Contribution.Status.IN_PROGRESS, fromDb.get().getStatus());
  }

  @Test
  void return409HttpCode_whenStartingInstanceIterationForNonExistingCentralServer(){
    doNothing().when(client).startInitialContribution(any(InstanceIterationRequest.class));

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/contributions", HttpEntity.EMPTY, Void.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
  }
}
