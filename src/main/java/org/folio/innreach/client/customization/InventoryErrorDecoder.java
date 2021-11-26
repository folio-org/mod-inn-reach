package org.folio.innreach.client.customization;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.io.IOException;
import java.util.Objects;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpStatus;

import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.util.JsonHelper;

@Log4j2
@RequiredArgsConstructor
public class InventoryErrorDecoder implements ErrorDecoder {

  private static final String CONFLICT_ERR_CODE = "23F09";

  private final ErrorDecoder defaultDecoder;
  private final JsonHelper jsonHelper;


  @Override
  public Exception decode(String methodKey, Response response) {
    log.debug("Decoding error from method call [{}] with response status [{}]", methodKey, response.status());

    Exception result = getIfNull(decodeConflict(response), () -> defaultDecoder.decode(methodKey, response));

    log.debug("Decoded error: {}", Objects.toString(result));

    return result;
  }

  private Exception decodeConflict(Response response) {
    Exception result = null;

    int status = response.status();
    if (status == HttpStatus.SC_CONFLICT || status == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
      var error = parseError(response);

      if (isConflict(error)) {
        result = new ResourceVersionConflictException(error.getMessage());

        log.debug("'Conflict' error detected with message: {}", error.getMessage());
      }
    }

    return result;
  }

  private boolean isConflict(InventoryError error) {
    return error != null && CONFLICT_ERR_CODE.equalsIgnoreCase(error.getCode());
  }

  private InventoryError parseError(Response response) {
    try {
      return jsonHelper.fromJson(response.body().asInputStream(), InventoryError.class);
    } catch (IOException | IllegalStateException e) {
      return null;
    }
  }

  @Data
  @NoArgsConstructor
  static class InventoryError {

    private String message;
    private String severity;
    private String code;

  }

}