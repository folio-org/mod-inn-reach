package org.folio.innreach.controller.d2ir;

import static org.folio.innreach.fixture.PatronFixture.createUserWithoutExpirationDate;
import static org.folio.innreach.fixture.PatronFixture.createUser;
import static org.folio.innreach.fixture.PatronFixture.createCustomFieldMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.PatronInfoRequestFixture.createPatronInfoRequest;
import static org.folio.innreach.fixture.TestUtil.circHeaders;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.AutomatedPatronBlocksClient;
import org.folio.innreach.client.ManualPatronBlocksClient;
import org.folio.innreach.client.PatronClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.PatronInfoResponseDTO;

@Sql(scripts = {"classpath:db/patron-type-mapping/clear-patron-type-mapping-tables.sql",
  "classpath:db/user-custom-field-mapping/clear-user-custom-field-mapping.sql",
  "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD)
@SqlMergeMode(MERGE)
class PatronInfoControllerTest extends BaseApiControllerTest {
  @Autowired
  private TestRestTemplate testRestTemplate;
  @MockBean
  private UsersClient usersClient;
  @MockBean
  private AutomatedPatronBlocksClient automatedBlocksClient;
  @MockBean
  private ManualPatronBlocksClient manualBlocksClient;
  @MockBean
  private PatronClient patronClient;
  @Mock
  private UserCustomFieldMappingService userCustomFieldService;

  private HttpHeaders headers = circHeaders();

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_when_patronFoundAndRequestAllowed() {
    var user = createUser();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_when_patronFoundWithNoExpirationDateAndRequestAllowed() {
    var user = createUserWithoutExpirationDate();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertNotNull(response.getPatronInfo().getPatronExpireDate());
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithoutPatronInfo_when_patronFoundAndRequestNotAllowed() {
    var user = createUser();
    var block = new AutomatedPatronBlocksClient.AutomatedPatronBlock();
    block.setBlockBorrowing(true);
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.of(1,
      List.of(block)));
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertFalse(response.getRequestAllowed());
    assertNull(response.getPatronInfo());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithError_when_verificationRequestFails() {
    when(usersClient.query(anyString())).thenThrow(new IllegalArgumentException("Test exception"));

    var patronInfoRequest = createPatronInfoRequest();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getMessages().get(0).contains("Test exception"));
  }
}
