package org.folio.innreach.client.config;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.util.JsonHelper;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class InventoryErrorHandler {

  private static final String CONFLICT_ERR_CODE = "23F09";

  private final JsonHelper jsonHelper;

  public void handle(ClientHttpResponse response) {
    var error = parseError(response);
    if (isConflict(error)) {
      log.debug("'Conflict' error detected with message: {}", error.getMessage());

      throw new ResourceVersionConflictException(error.getMessage());
    }
  }

  private InventoryError parseError(ClientHttpResponse response) {
    try {
      return jsonHelper.fromJson(response.getBody(), InventoryError.class);
    } catch (IOException | IllegalStateException e) {
      return null;
    }
  }

  private boolean isConflict(InventoryError error) {
    return error != null && CONFLICT_ERR_CODE.equalsIgnoreCase(error.getCode());
  }

  @Data
  @AllArgsConstructor(access = AccessLevel.PACKAGE)
  @NoArgsConstructor
  static class InventoryError {

    private String message;
    private String severity;
    private String code;

  }
}
