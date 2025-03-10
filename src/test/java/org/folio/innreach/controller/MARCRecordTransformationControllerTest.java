package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

@Sql(
  scripts = {
    "classpath:db/marc-transform-opt-set/clear-marc-transform-opt-set-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MARCRecordTransformationControllerTest extends BaseControllerTest {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");

  @Autowired
  private TestRestTemplate testRestTemplate;

  @MockitoBean
  private InstanceStorageClient instanceStorageClient;

  @MockitoBean
  private SourceRecordStorageClient sourceRecordStorageClient;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void returnTransformedMARCRecord() {
    when(instanceStorageClient.getInstanceById(any()))
      .thenReturn(deserializeFromJsonFile("/inventory-storage/american-bar-association.json", Instance.class));

    when(sourceRecordStorageClient.getRecordByInstanceId(any()))
      .thenReturn(deserializeFromJsonFile("/source-record-storage/source-record-storage-example.json", SourceRecordDTO.class));

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-record-transformation/{inventoryInstanceId}",
      TransformedMARCRecordDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, UUID.randomUUID());

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);
    assertNotNull(body.getContent());
    assertNotNull(body.getBase64rawContent());
    String decodedString = new String(Base64.getDecoder().decode(body.getBase64rawContent()), StandardCharsets.UTF_8);
    assertTrue(decodedString.contains("©Ø"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set-inactive.sql"
  })
  void returnTransformedMARCRecord_inactiveConfig() {
    when(instanceStorageClient.getInstanceById(any()))
      .thenReturn(deserializeFromJsonFile("/inventory-storage/american-bar-association.json", Instance.class));

    when(sourceRecordStorageClient.getRecordByInstanceId(any()))
      .thenReturn(deserializeFromJsonFile("/source-record-storage/source-record-storage-example.json", SourceRecordDTO.class));

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-record-transformation/{inventoryInstanceId}",
      TransformedMARCRecordDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, UUID.randomUUID());

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);
    assertNotNull(body.getContent());
    assertNotNull(body.getBase64rawContent());
  }

}
