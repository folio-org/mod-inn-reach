package org.folio.innreach.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.folio.innreach.dto.AuthenticationRequest;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;

@Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
@Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql", executionPhase = AFTER_TEST_METHOD)
class AuthenticationControllerIT extends BaseTenantIT {

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  void return200HttpCode_when_localServerCredentialsAreSuccessfullyAuthenticated() throws Exception {
    var authenticationRequest = deserializeFromJsonFile("/authentication/authentication-request.json",
        AuthenticationRequest.class);

    mockMvc.perform(post("/inn-reach/authentication")
        .content(asJsonString(authenticationRequest))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  void return401HttpCode_when_localServerCredentialsAreNotAuthenticated() throws Exception {
    var authenticationRequest = deserializeFromJsonFile("/authentication/bad-credentials-authentication-request.json",
        AuthenticationRequest.class);

    mockMvc.perform(post("/inn-reach/authentication")
        .content(asJsonString(authenticationRequest))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }
}
