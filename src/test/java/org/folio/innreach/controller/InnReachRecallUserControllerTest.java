package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.dto.InnReachRecallUserDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIntegrationTest;
import org.folio.innreach.repository.CentralServerRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Sql(scripts = {
  "classpath:db/central-server/clear-central-server-tables.sql",
  "classpath:db/inn-reach-recall-user/clear-inn-reach-recall-user.sql"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class InnReachRecallUserControllerTest extends BaseTenantIntegrationTest {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c697-42b2-89bb-1bbb8759b0d3");

  private static final String CENTRAL_SERVER_RECALL_USER_URI = "/inn-reach/central-servers/{id}/inn-reach-recall-user";

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;
  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void returnRecallUser_when_centralUserRecallUserExists() throws Exception {
    var mvcResult = mockMvc.perform(get(CENTRAL_SERVER_RECALL_USER_URI, PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();
    var body = fromJson(mvcResult, InnReachRecallUserDTO.class);
    assertNotNull(body);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void returnErrorMessage_when_centralServerRecallUserDoesNotExist() throws Exception {
    var mvcResult = mockMvc.perform(get(CENTRAL_SERVER_RECALL_USER_URI, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound())
      .andReturn();
    var body = fromJson(mvcResult, InnReachRecallUserDTO.class);
    assertNotNull(body);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void saveRecallUser() throws Exception {
    var innReachRecallUserDTO = new InnReachRecallUserDTO();
    innReachRecallUserDTO.setUserId(UUID.randomUUID());

    var mvcResult = mockMvc.perform(post(CENTRAL_SERVER_RECALL_USER_URI, PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID)
        .content(asJsonString(innReachRecallUserDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    var body = fromJson(mvcResult, InnReachRecallUserDTO.class);
    assertNotNull(body);

    var centralServer = centralServerRepository.fetchOneWithRecallUser(PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID).orElseThrow();
    var innReachRecallUser = centralServer.getInnReachRecallUser();

    assertNotNull(innReachRecallUser);
    assertEquals(body.getUserId(), innReachRecallUser.getUserId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void updateRecallUser() throws Exception {
    var innReachRecallUserDTO = new InnReachRecallUserDTO();
    innReachRecallUserDTO.setUserId(UUID.randomUUID());

    mockMvc.perform(put(CENTRAL_SERVER_RECALL_USER_URI, PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID)
        .content(asJsonString(innReachRecallUserDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var centralServer = centralServerRepository.fetchOneWithRecallUser(PRE_POPULATED_CENTRAL_SERVER_WITH_RECALL_USER_ID).orElseThrow();
    var innReachRecallUser = centralServer.getInnReachRecallUser();

    assertNotNull(innReachRecallUser);
    assertEquals(innReachRecallUserDTO.getUserId(), innReachRecallUser.getUserId());
  }

}
