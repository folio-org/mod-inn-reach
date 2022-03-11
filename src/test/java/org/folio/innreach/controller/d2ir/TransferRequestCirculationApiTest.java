package org.folio.innreach.controller.d2ir;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.d2ir.CirculationResultUtils.emptyErrors;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.exceptionMatch;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.failedWithReason;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.logResponse;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.fixture.CirculationFixture.createTransferRequestDTO;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric32Max;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric5;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.dto.CentralServerDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
})
@Sql(scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql",
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class TransferRequestCirculationApiTest extends BaseApiControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_ITEM_ID = "item1";
  private static final String PRE_POPULATED_ITEM_AGENCY_CODE = "asd34";
  private static final String NEW_ITEM_ID = "newitem";
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATE_PATRON_GROUP_ID = UUID.fromString("8534295a-e031-4738-a952-f7db900df8c0");
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 122;

  private static final String TRANSFERREQ_URL = "/inn-reach/d2ir/circ/transferrequest/{trackingId}/{centralCode}";

  @Autowired
  private InnReachTransactionRepository repository;

  @MockBean
  private UserService userService;
  @MockBean
  private PatronTypeMappingService patronTypeMappingService;
  @MockBean
  private CentralServerService centralServerService;

  @Test
  void updateTransactionItemId_with_newItemFromRequest() throws Exception {
    var req = createTransferRequest();
    req.setNewItemId(NEW_ITEM_ID);
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    putAndExpectOk(transferReqUri(), req);

    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(NEW_ITEM_ID, trx.getHold().getItemId());
    assertEquals(TRANSFER, trx.getState());
  }

  @ParameterizedTest
  @MethodSource("transactionNotFoundArgProvider")
  void return400_when_TransactionNotFound(String trackingId, String centralCode, TransferRequestDTO req)
      throws Exception {
    putReq(transferReqUri(trackingId, centralCode), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(trackingId), containsString(centralCode)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(EntityNotFoundException.class));
  }

  @Test
  void return400_when_ItemIdDoesntMatch() throws Exception {
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));
    var req = createTransferRequest();
    req.setItemId(randomAlphanumeric32Max());

    putReq(transferReqUri(), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(req.getItemId()), containsString(PRE_POPULATED_ITEM_ID)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(IllegalArgumentException.class));
  }

  @Test
  void return400_when_ItemAgencyCodeDoesntMatch() throws Exception {
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));
    var req = createTransferRequest();
    req.setItemAgencyCode(randomAlphanumeric5());

    putReq(transferReqUri(), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(req.getItemAgencyCode()), containsString(PRE_POPULATED_ITEM_AGENCY_CODE)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(IllegalArgumentException.class));
  }

  private static TransferRequestDTO createTransferRequest() {
    var req = createTransferRequestDTO();

    req.setItemId(PRE_POPULATED_ITEM_ID);
    req.setItemAgencyCode(PRE_POPULATED_ITEM_AGENCY_CODE);

    return req;
  }

  static Stream<Arguments> transactionNotFoundArgProvider() {
    return Stream.of(
        arguments(PRE_POPULATED_TRACKING_ID, randomAlphanumeric5(), createTransferRequestDTO()),
        arguments(randomAlphanumeric32Max(), PRE_POPULATED_CENTRAL_CODE, createTransferRequestDTO())
    );
  }

  private static URI transferReqUri() {
    return transferReqUri(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private static URI transferReqUri(String trackingId, String centralCode) {
    return URI.of(TRANSFERREQ_URL, trackingId, centralCode);
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow();
  }

  private void putAndExpectOk(URI uri, Object requestBody) throws Exception {
    putAndExpect(uri, requestBody, Template.of("circulation/ok-response.json"));
  }

  private User populateUser() {
    var user = new User();
    user.setId(PRE_POPULATED_PATRON_ID);
    user.setPatronGroupId(PRE_POPULATE_PATRON_GROUP_ID);
    var personal = new User.Personal();
    personal.setPreferredFirstName("Paul");
    personal.setFirstName("MuaDibs");
    personal.setLastName("Atreides");
    user.setPersonal(personal);
    return user;
  }

}
