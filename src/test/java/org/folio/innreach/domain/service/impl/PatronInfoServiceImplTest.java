package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.folio.innreach.domain.service.CentralPatronTypeMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.external.mapper.InnReachResponseMapper;
import org.folio.innreach.external.mapper.InnReachResponseMapperImpl;
import org.folio.innreach.fixture.CentralServerFixture;

@ExtendWith(MockitoExtension.class)
class PatronInfoServiceImplTest {

  public static final UUID USER_ID = UUID.randomUUID();
  public static final String PATRON_NAME = "John Doe";
  public static final Integer CENTRAL_PATRON_TYPE = 42;
  public static final Integer TOTAL_LOANS = 7;
  public static final Integer INN_REACH_LOANS = 3;
  public static final String CENTRAL_CODE = "d2ir";
  public static final String VISIBLE_PATRON_ID = "111111";
  public static final String AGENCY_CODE = "test1";
  private static final long expiryDateTs = System.currentTimeMillis();

  @Mock
  private UserServiceImpl userService;
  @Mock
  private CentralPatronTypeMappingService centralPatronTypeMappingService;
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

  @Spy
  private InnReachResponseMapper mapper = new InnReachResponseMapperImpl();

  @InjectMocks
  private PatronInfoServiceImpl service;

  @Test
  void shouldReturnPatronInfo() {
    var user = new User();
    user.setId(USER_ID.toString());
    user.setActive(true);
    user.setExpirationDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiryDateTs), ZoneOffset.UTC));
    user.setPersonal(User.Personal.of("John", null, "Doe", null));

    when(centralServerService.getCentralServerByCentralCode(any())).thenReturn(CentralServerFixture.createCentralServerDTO());
    when(userService.getUserByPublicId(any())).thenReturn(Optional.of(user));
    when(patronBlocksClient.getPatronBlocks(any())).thenReturn(ResultList.empty());
    when(patronClient.getAccountDetails(any())).thenReturn(PatronDTO.of(TOTAL_LOANS, singletonList(Loan.of(UUID.randomUUID()))));
    when(transactionService.countInnReachLoans(any(), any())).thenReturn(INN_REACH_LOANS);
    when(centralPatronTypeMappingService.getCentralPatronType(any(), any())).thenReturn(CENTRAL_PATRON_TYPE);

    var response = service.verifyPatron(CENTRAL_CODE, VISIBLE_PATRON_ID, AGENCY_CODE, PATRON_NAME);

    assertNotNull(response);
    var patronInfo = response.getPatronInfo();

    assertNotNull(patronInfo);
    assertEquals(USER_ID.toString().replaceAll("-", ""), patronInfo.getPatronId());
    assertEquals(PATRON_NAME, patronInfo.getPatronName());
    assertEquals(CENTRAL_PATRON_TYPE, patronInfo.getCentralPatronType());
    assertEquals(INN_REACH_LOANS, patronInfo.getNonLocalLoans());
    assertEquals(TOTAL_LOANS - INN_REACH_LOANS, (int) patronInfo.getLocalLoans());
    assertEquals(AGENCY_CODE, patronInfo.getPatronAgencyCode());
    assertNotNull(patronInfo.getPatronExpireDate());
  }
}
