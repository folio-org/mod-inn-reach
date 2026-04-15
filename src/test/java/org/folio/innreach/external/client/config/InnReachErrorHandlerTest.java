package org.folio.innreach.external.client.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@ExtendWith(MockitoExtension.class)
class InnReachErrorHandlerTest {

  private static final String ERROR_MESSAGE = "Resource not found";
  private static final String SERVER_ERROR_JSON = "{\"message\":\"Internal server error\"}";
  private static final String CLIENT_ERROR_JSON = "{\"message\":\"Resource not found\"}";
  private static final String UNAUTHORIZED_ERROR_JSON = "{\"message\":\"Invalid credentials\"}";

  @Mock
  private JsonHelper jsonHelper;

  @Mock
  private ClientHttpResponse response;

  @InjectMocks
  private InnReachErrorHandler errorHandler;

  @Test
  void handle_withUnauthorizedStatus_throwsBadCredentialsException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(UNAUTHORIZED_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenReturn(createHttpClientErrorException(ERROR_MESSAGE));

    var exception = assertThrows(BadCredentialsException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("Can't get InnReach access token"));
    assertTrue(exception.getMessage().contains("Key/Secret pair is not valid"));
  }

  @Test
  void handle_withBadGatewayStatus_throwsInnReachGatewayException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(SERVER_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenReturn(createHttpServerErrorException("Bad gateway"));

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN_Reach call failed"));
    assertTrue(exception.getMessage().contains("502"));
  }

  @Test
  void handle_withGatewayTimeoutStatus_throwsInnReachGatewayException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.GATEWAY_TIMEOUT);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(SERVER_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenReturn(createHttpServerErrorException("Gateway timeout"));

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN_Reach call failed"));
    assertTrue(exception.getMessage().contains("504"));
  }

  @Test
  void handle_withInternalServerErrorStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(SERVER_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenReturn(createHttpServerErrorException(ERROR_MESSAGE));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_withBadRequestStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CLIENT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenReturn(createHttpClientErrorException(ERROR_MESSAGE));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_withServiceUnavailableStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(SERVER_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenReturn(createHttpServerErrorException("Service unavailable"));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_withUnauthorizedStatusAndParseError_throwsBadCredentialsExceptionWithEmptyMessage() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenThrow(new IllegalStateException("Parse error"));

    var exception = assertThrows(BadCredentialsException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("Can't get InnReach access token"));
  }

  @Test
  void handle_withBadGatewayStatusAndParseError_throwsInnReachGatewayException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenThrow(new IOException("IO error"));

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN_Reach call failed"));
  }

  @Test
  void handle_withNonGatewayErrorStatusAndParseError_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenThrow(new IllegalStateException("Parse error"));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_withNotFoundStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CLIENT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenReturn(createHttpClientErrorException("Not found"));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_withForbiddenStatus_throwsInnReachException() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CLIENT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenReturn(createHttpClientErrorException("Forbidden"));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("INN-Reach call failed"));
  }

  @Test
  void handle_with5xxServerError_extractsServerErrorMessage() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(SERVER_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class))
      .thenReturn(createHttpServerErrorException("Server error"));

    var exception = assertThrows(InnReachGatewayException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains("Server error"));
  }

  @Test
  void handle_with4xxClientError_extractsClientErrorMessage() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CLIENT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class))
      .thenReturn(createHttpClientErrorException(ERROR_MESSAGE));

    var exception = assertThrows(InnReachException.class, () -> errorHandler.handle(response));

    assertTrue(exception.getMessage().contains(ERROR_MESSAGE));
  }

  private HttpServerErrorException createHttpServerErrorException(String message) {
    return new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  private HttpClientErrorException createHttpClientErrorException(String message) {
    return new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
  }
}

