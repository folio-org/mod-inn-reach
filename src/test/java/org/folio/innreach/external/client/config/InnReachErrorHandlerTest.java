package org.folio.innreach.external.client.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.util.JsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class InnReachErrorHandlerTest {

  @Mock
  private JsonHelper jsonHelper;

  @Mock
  private ClientHttpResponse response;

  @InjectMocks
  private InnReachErrorHandler errorHandler;

  @Test
  void handle_withUnauthorizedStatus_throwsBadCredentialsException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);

    var exception = assertThrows(BadCredentialsException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("Can't get InnReach access token"));
    assertTrue(exception.getMessage().contains("Key/Secret pair is not valid"));
  }

  @Test
  void handle_withBadGatewayStatus_throwsInnReachGatewayException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN_Reach call failed"));
    assertTrue(exception.getMessage().contains("502"));
  }

  @Test
  void handle_withGatewayTimeoutStatus_throwsInnReachGatewayException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.GATEWAY_TIMEOUT);

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN_Reach call failed"));
    assertTrue(exception.getMessage().contains("504"));
  }

  @Test
  void handle_withInternalServerErrorStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
    assertTrue(exception.getMessage().contains("500"));
  }

  @Test
  void handle_withBadRequestStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
    assertTrue(exception.getMessage().contains("400"));
  }

  @Test
  void handle_withServiceUnavailableStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
    assertTrue(exception.getMessage().contains("503"));
  }

  @Test
  void handle_withNotFoundStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
    assertTrue(exception.getMessage().contains("404"));
  }

  @Test
  void handle_withForbiddenStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
    assertTrue(exception.getMessage().contains("403"));
  }
}
