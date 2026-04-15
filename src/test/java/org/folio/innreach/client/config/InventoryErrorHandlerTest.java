package org.folio.innreach.client.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.util.JsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.ClientHttpResponse;

@ExtendWith(MockitoExtension.class)
class InventoryErrorHandlerTest {

  private static final String CONFLICT_ERR_CODE = "23F09";
  private static final String CONFLICT_ERROR_JSON = "{\"message\":\"Resource conflict\",\"severity\":\"error\",\"code\":\"23F09\"}";
  private static final String NON_CONFLICT_ERROR_JSON = "{\"message\":\"Bad request\",\"severity\":\"error\",\"code\":\"400\"}";
  private static final String CONFLICT_ERROR_MESSAGE = "Resource conflict";

  @Mock
  private JsonHelper jsonHelper;

  @Mock
  private ClientHttpResponse response;

  @InjectMocks
  private InventoryErrorHandler errorHandler;

  @Test
  void handle_withConflictError_throwsResourceVersionConflictException() throws IOException {
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CONFLICT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), InventoryErrorHandler.InventoryError.class))
      .thenReturn(new InventoryErrorHandler.InventoryError(CONFLICT_ERROR_MESSAGE, "error", CONFLICT_ERR_CODE));

    var exception = assertThrows(ResourceVersionConflictException.class, () -> errorHandler.handle(response));

    assert exception.getMessage().equals(CONFLICT_ERROR_MESSAGE);
  }

  @Test
  void handle_withNonConflictError_doesNotThrowException() throws IOException {
    when(response.getBody()).thenReturn(new ByteArrayInputStream(NON_CONFLICT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(response.getBody(), InventoryErrorHandler.InventoryError.class))
      .thenReturn(new InventoryErrorHandler.InventoryError("Bad request", "error", "400"));

    assertDoesNotThrow(() -> errorHandler.handle(response));
  }

  @Test
  void handle_withIOException_doesNotThrowException() throws IOException {
    when(response.getBody()).thenThrow(new IOException("IO error"));

    assertDoesNotThrow(() -> errorHandler.handle(response));
  }


  @Test
  void handle_withNullResponseBody_doesNotThrowException() throws IOException {
    when(response.getBody()).thenReturn(null);

    assertDoesNotThrow(() -> errorHandler.handle(response));
  }

  @Test
  void handle_withNullErrorObject_doesNotThrowException() throws IOException {
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(jsonHelper.fromJson(isA(ByteArrayInputStream.class), any()))
      .thenReturn(null);

    assertDoesNotThrow(() -> errorHandler.handle(response));
  }

  @Test
  void handle_withConflictCodeCaseInsensitive_throwsResourceVersionConflictException() throws IOException {
    var lowerCaseConflictJson = "{\"message\":\"Resource conflict\",\"severity\":\"error\",\"code\":\"23f09\"}";
    when(response.getBody()).thenReturn(new ByteArrayInputStream(lowerCaseConflictJson.getBytes()));
    when(jsonHelper.fromJson(isA(ByteArrayInputStream.class), any()))
      .thenReturn(new InventoryErrorHandler.InventoryError(CONFLICT_ERROR_MESSAGE, "error", "23f09"));

    var exception = assertThrows(ResourceVersionConflictException.class, () -> errorHandler.handle(response));

    assert exception.getMessage().equals(CONFLICT_ERROR_MESSAGE);
  }

  @Test
  void handle_withConflictErrorAndEmptyMessage_throwsResourceVersionConflictException() throws IOException {
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CONFLICT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(isA(ByteArrayInputStream.class), any()))
      .thenReturn(new InventoryErrorHandler.InventoryError("", "error", CONFLICT_ERR_CODE));

    var exception = assertThrows(ResourceVersionConflictException.class, () -> errorHandler.handle(response));

    assert exception.getMessage().isEmpty();
  }

  @Test
  void handle_withConflictErrorAndNullMessage_throwsResourceVersionConflictException() throws IOException {
    when(response.getBody()).thenReturn(new ByteArrayInputStream(CONFLICT_ERROR_JSON.getBytes()));
    when(jsonHelper.fromJson(isA(ByteArrayInputStream.class), any()))
      .thenReturn(new InventoryErrorHandler.InventoryError(null, "error", CONFLICT_ERR_CODE));

    assertThrows(ResourceVersionConflictException.class, () -> errorHandler.handle(response));
  }
}

