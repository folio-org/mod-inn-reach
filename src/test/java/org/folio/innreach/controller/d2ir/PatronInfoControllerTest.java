package org.folio.innreach.controller.d2ir;

import static org.folio.innreach.fixture.PatronFixture.PATRON_FIRST_NAME;
import static org.folio.innreach.fixture.PatronFixture.PATRON_LAST_NAME;
import static org.folio.innreach.fixture.PatronFixture.createUserWithTwoFirstAndTwoLastNames;
import static org.folio.innreach.fixture.PatronFixture.createUserWithoutExpirationDate;
import static org.folio.innreach.fixture.PatronFixture.createUser;
import static org.folio.innreach.fixture.PatronFixture.createUserWithMiddleName;
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
import static org.folio.innreach.fixture.PatronInfoRequestFixture.createPatronInfoRequestWithLastNameFirstName;
import static org.folio.innreach.fixture.PatronInfoRequestFixture.createPatronInfoRequestWithFirstNameMiddleNameLastName;
import static org.folio.innreach.fixture.PatronInfoRequestFixture.createPatronInfoRequestWithLastNameFirstNameMiddleName;
import static org.folio.innreach.fixture.TestUtil.circHeaders;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
  public static final String UNABLE_TO_VERIFY_PATRON = "Unable to verify patron";
  @Autowired
  private TestRestTemplate testRestTemplate;
  @MockitoBean
  private UsersClient usersClient;
  @MockitoBean
  private AutomatedPatronBlocksClient automatedBlocksClient;
  @MockitoBean
  private ManualPatronBlocksClient manualBlocksClient;
  @MockitoBean
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
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasLastNameFirstNameOrder_when_patronFoundAndRequestAllowed() {
    var user = createUser();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequestWithLastNameFirstName();

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
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasFirstNameMiddleNameLastNameOrder_when_patronFoundAndRequestAllowed() {
    var user = createUserWithMiddleName();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequestWithFirstNameMiddleNameLastName();

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
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasLastNameFirstNameMiddleNameOrder_when_patronFoundAndRequestAllowed() {
    var user = createUserWithMiddleName();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequestWithLastNameFirstNameMiddleName();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"John Paul Test","John Test Doe","John Test", "John,Paul Test","John Paul,Test","John,Paul,Test",
    ",John,Paul,Test,", "John,"})

  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasWrongOrder_when_patronNotFound(String updatedName) {
    var user = createUserWithMiddleName();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequestWithLastNameFirstNameMiddleName();
    patronInfoRequest.setPatronName(updatedName);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertFalse(response.getRequestAllowed());
    assertEquals(UNABLE_TO_VERIFY_PATRON,response.getReason());
  }

  @ParameterizedTest
  @ValueSource(strings = {"john doe","doe john","doe john", "john, doe", ",john, doe","john doe,",",john, doe,"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasIgnoreCaseCorrectOrderWithOutMiddleName_when_patronFoundAndRequestAllowed(String patronName) {
    var user = createUser();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();
    patronInfoRequest.setPatronName(patronName);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"john paul doe","doe john paul","doe John pauL","doe, John pauL","doe John, pauL",
    "doe John pauL,", "doe, John, pauL", "doe John, pauL,", ",doe, John, pauL,", "John, Doe "," John, Doe ",
    "  Doe,   John    ", " John  paul  doe "})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_hasIgnoreCaseCorrectOrderWithMiddleName_when_patronFoundAndRequestAllowed(String patronName) {
    var user = createUserWithMiddleName();
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();
    patronInfoRequest.setPatronName(patronName);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"john jimmy doe smith","doe smith john jimmy","john jimmy doe smith pauL",
    "doe smith john jimmy pauL","doe smith, john jimmy, pauL", "doe smith john jimmy, pauL"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_WithSpaceInFirstAndLastName(String patronName) {
    // This test covers "first first last last", "last last first first", "first first last last middle"
    // "last last, first first middle"
    var user = createUserWithTwoFirstAndTwoLastNames("john jimmy", "doe smith");
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();
    patronInfoRequest.setPatronName(patronName);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"john jimmy doe","doe john jimmy paul", "doe, john jimmy paul", "john jimmy doe pauL",
    "doe john jimmy", "doe, john jimmy"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_WithSpaceInFirstName(String patronName) {
    // This test covers "first first last", "last first first middle", "last, first first middle",
    // "first first last middle", "last first first", "last, first first"
    var user = createUserWithTwoFirstAndTwoLastNames("john jimmy", PATRON_LAST_NAME);
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();
    patronInfoRequest.setPatronName(patronName);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/verifypatron", new HttpEntity<>(patronInfoRequest, headers), PatronInfoResponseDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    var response = responseEntity.getBody();
    assertTrue(response.getRequestAllowed());
    assertNotNull(response.getPatronInfo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"doe smith john","john doe smith paul","doe smith john pauL",
    "doe smith, john pauL", "john doe smith"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return200HttpCode_and_patronInfoResponseWithPatronInfo_WithSpaceInLastName(String patronName) {
    // This test covers "last last first", "first last last middle", "last last first middle",
    // "last last, first middle", "first last last"
    var user = createUserWithTwoFirstAndTwoLastNames(PATRON_FIRST_NAME, "doe smith");
    user.setPatronGroupId(UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce"));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    when(automatedBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(manualBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(new PatronDTO());
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var patronInfoRequest = createPatronInfoRequest();
    patronInfoRequest.setPatronName(patronName);

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
