package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;

@Sql(
  scripts = {
    "classpath:db/marc-transform-opt-set/clear-marc-transform-opt-set-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MARCRecordTransformationControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID = "51768f15-41e8-494d-bc4d-a308568e7052";
  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");

  @Autowired
  private TestRestTemplate testRestTemplate;

  @MockBean
  private InventoryClient inventoryClient;

  @MockBean
  private SourceRecordStorageClient sourceRecordStorageClient;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void returnTransformedMARCRecord() {
    when(inventoryClient.getInstanceById(any()))
      .thenReturn(deserializeFromJsonFile("/inventory-storage/american-bar-association.json", InventoryInstanceDTO.class));

    when(sourceRecordStorageClient.getRecordById(any()))
      .thenReturn(deserializeFromJsonFile("/source-record-storage/source-record-storage-example.json", SourceRecordDTO.class));

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-record-transformation/{inventoryInstanceId}", Void.class,
      PRE_POPULATED_CENTRAL_SERVER_ID, UUID.randomUUID());

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }
}
