package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.dto.folio.ResultList.asSinglePage;
import static org.folio.innreach.domain.service.impl.PatronInfoServiceImpl.ERROR_REASON;
import static org.folio.innreach.external.dto.InnReachResponse.ERROR_STATUS;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerDTO;
import static org.folio.innreach.fixture.PatronFixture.PATRON_FIRST_NAME;
import static org.folio.innreach.fixture.PatronFixture.createPatronBlock;
import static org.folio.innreach.fixture.PatronFixture.createUser;
import static org.folio.innreach.fixture.PatronFixture.getErrorMsg;
import static org.folio.innreach.fixture.PatronFixture.getPatronId;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.PatronBlocksClient;
import org.folio.innreach.client.PatronClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO.Loan;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.external.mapper.InnReachResponseMapper;
import org.folio.innreach.external.mapper.InnReachResponseMapperImpl;

@ExtendWith(MockitoExtension.class)
class PatronInfoServiceImplTest {

  public static final String PATRON_NAME = "John Doe";
  public static final String FOLIO_PATRON_NAME = "Doe, John";
  public static final Integer CENTRAL_PATRON_TYPE = 42;
  public static final Integer TOTAL_LOANS = 7;
  public static final Integer INN_REACH_LOANS = 3;
  public static final String CENTRAL_CODE = "d2ir";
  public static final String VISIBLE_PATRON_ID = "111111";
  public static final String AGENCY_CODE = "test1";
  public static final String CENTRAL_AGENCY_CODE = "code1";

  @Mock
  private UserServiceImpl userService;
  @Mock
  private PatronTypeMappingService patronTypeMappingService;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private InnReachTransactionService transactionService;
  @Mock
  private PatronClient patronClient;
  @Mock
  private PatronBlocksClient patronBlocksClient;
  @Mock
  private UserCustomFieldMappingService userCustomFieldService;

  @Spy
  private InnReachResponseMapper mapper = new InnReachResponseMapperImpl();

  @InjectMocks
  private PatronInfoServiceImpl service;

  @Test
  void shouldReturnPatronInfo() {
    var user = createUser();
    var patronId = getPatronId(user);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(PatronDTO.of(TOTAL_LOANS, singletonList(Loan.of(UUID.randomUUID()))));
    when(transactionService.countInnReachLoans(any(), any())).thenReturn(INN_REACH_LOANS);
    when(patronTypeMappingService.getCentralPatronType(any(), any())).thenReturn(Optional.of(CENTRAL_PATRON_TYPE));
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    var patronInfo = response.getPatronInfo();

    assertNotNull(patronInfo);
    assertEquals(patronId, patronInfo.getPatronId());
    assertEquals(FOLIO_PATRON_NAME, patronInfo.getPatronName());
    assertEquals(CENTRAL_PATRON_TYPE, patronInfo.getCentralPatronType());
    assertEquals(INN_REACH_LOANS, patronInfo.getNonLocalLoans());
    assertEquals(TOTAL_LOANS - INN_REACH_LOANS, (int) patronInfo.getLocalLoans());
    assertEquals(CENTRAL_AGENCY_CODE, patronInfo.getPatronAgencyCode());
    assertNotNull(patronInfo.getPatronExpireDate());
  }

  @Test
  void shouldReturnPatronInfo_MatchFirstMiddleLastName() {
    var user = createUser();
    var p = user.getPersonal();
    p.setFirstName("Abc");
    p.setMiddleName("John");
    p.setLastName("Doe");
    var patronName = "Abc John Doe";
    var folioPatronName = "Doe, Abc John";

    var strUserId = getPatronId(user);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(PatronDTO.of(TOTAL_LOANS, singletonList(Loan.of(UUID.randomUUID()))));
    when(transactionService.countInnReachLoans(any(), any())).thenReturn(INN_REACH_LOANS);
    when(patronTypeMappingService.getCentralPatronType(any(), any())).thenReturn(Optional.of(CENTRAL_PATRON_TYPE));
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, patronName);

    assertNotNull(response);
    var patronInfo = response.getPatronInfo();

    assertNotNull(patronInfo);
    assertEquals(strUserId, patronInfo.getPatronId());
    assertEquals(folioPatronName, patronInfo.getPatronName());
    assertEquals(CENTRAL_PATRON_TYPE, patronInfo.getCentralPatronType());
    assertEquals(INN_REACH_LOANS, patronInfo.getNonLocalLoans());
    assertEquals(TOTAL_LOANS - INN_REACH_LOANS, (int) patronInfo.getLocalLoans());
    assertEquals(CENTRAL_AGENCY_CODE, patronInfo.getPatronAgencyCode());
    assertNotNull(patronInfo.getPatronExpireDate());
  }

  @Test
  void shouldReturnPatronInfo_MatchMiddleLastName() {
    var user = createUser();
    user.getPersonal().setPreferredFirstName("Abc");
    user.getPersonal().setMiddleName("John");
    user.getPersonal().setLastName("Doe");
    var patronName = "John Doe";
    var folioPatronName = "Doe, Abc John";

    var strUserId = getPatronId(user);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(PatronDTO.of(TOTAL_LOANS, singletonList(Loan.of(UUID.randomUUID()))));
    when(transactionService.countInnReachLoans(any(), any())).thenReturn(INN_REACH_LOANS);
    when(patronTypeMappingService.getCentralPatronType(any(), any())).thenReturn(Optional.of(CENTRAL_PATRON_TYPE));
    when(userCustomFieldService.getMapping(any())).thenReturn(createCustomFieldMapping());

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, patronName);

    assertNotNull(response);
    var patronInfo = response.getPatronInfo();

    assertNotNull(patronInfo);
    assertEquals(strUserId, patronInfo.getPatronId());
    assertEquals(folioPatronName, patronInfo.getPatronName());
    assertEquals(CENTRAL_PATRON_TYPE, patronInfo.getCentralPatronType());
    assertEquals(INN_REACH_LOANS, patronInfo.getNonLocalLoans());
    assertEquals(TOTAL_LOANS - INN_REACH_LOANS, (int) patronInfo.getLocalLoans());
    assertEquals(CENTRAL_AGENCY_CODE, patronInfo.getPatronAgencyCode());
    assertNotNull(patronInfo.getPatronExpireDate());
  }

  @Test
  void shouldReturnError_PatronNotFound() {
    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("Patron is not found by visiblePatronId: " + VISIBLE_PATRON_ID, getErrorMsg(response));
  }

  @Test
  void shouldReturnError_PatronNotFoundByName() {
    var user = createUser();
    user.getPersonal().setFirstName("Le " + PATRON_FIRST_NAME);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("Patron is not found by name: " + PATRON_NAME, getErrorMsg(response));
  }

  @Test
  void shouldReturnError_PatronBlocksFound() {
    var patronBlock = createPatronBlock();
    patronBlock.setBlockBorrowing(true);
    patronBlock.setBlockRequests(false);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(createUser()));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(asSinglePage(patronBlock));

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("Patron block found: " + patronBlock.getMessage(), getErrorMsg(response));
  }

  @Test
  void shouldReturnError_PatronBlockRequests() {
    var patronBlock = createPatronBlock();
    patronBlock.setBlockBorrowing(false);
    patronBlock.setBlockRequests(true);

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(createUser()));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(asSinglePage(patronBlock));

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("Patron block found: " + patronBlock.getMessage(), getErrorMsg(response));
  }

  @Test
  void shouldReturnError_CentralTypeNotFound() {
    User user = createUser();

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("centralPatronType is not resolved for patron with public id: " + user.getBarcode(), getErrorMsg(response));
  }

  @Test
  void shouldReturnError_InvalidPatronName() {
    var patronName = "testName";

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(createUser()));

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, patronName);

    assertNotNull(response);
    assertNull(response.getPatronInfo());
    assertFalse(response.getRequestAllowed());
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_REASON, response.getReason());
    assertEquals("Patron is not found by name: " + patronName, getErrorMsg(response));
  }

  private static UserCustomFieldMappingDTO createCustomFieldMapping() {
    var mapping = new UserCustomFieldMappingDTO();
    mapping.setConfiguredOptions(Map.of(AGENCY_CODE, CENTRAL_AGENCY_CODE));
    mapping.setId(UUID.randomUUID());
    mapping.setCustomFieldId(UUID.randomUUID());
    return mapping;
  }

}
