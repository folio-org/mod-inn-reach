package org.folio.innreach.external.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.CollectionUtils;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InnReachResponse {

  public static final String OK_STATUS = "ok";
  public static final String ERROR_STATUS = "failed";
  public static final String OK_REASON = "success";
  public static final String FIELD_ERROR_TYPE = "FieldError";
  public static final String INVALID_REQUEST_REASON = "Invalid request";
  public static final String INVALID_RECORD_KEY_REASON = "Invalid record key";

  protected String status;
  protected String reason;
  protected List<Error> errors;

  public static InnReachResponse okResponse() {
    return InnReachResponse.builder()
      .status(OK_STATUS)
      .reason(OK_REASON)
      .build();
  }

  public static InnReachResponse errorResponse() {
    return InnReachResponse.builder()
      .status(ERROR_STATUS)
      .reason(INVALID_REQUEST_REASON)
      .build();
  }

  public static InnReachResponse errorResponse(String reason, List<Error> errors) {
    return InnReachResponse.builder()
      .status(ERROR_STATUS)
      .reason(reason)
      .errors(errors)
      .build();
  }

  public boolean isOk() {
    return OK_STATUS.equals(status) && CollectionUtils.isEmpty(errors);
  }

  @Builder
  @AllArgsConstructor
  @Data
  @ToString
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Error {
    private String reason;
    private String central;
    /**
     * Batch processing returns errors instead of messages
     */
    private List<Map<String, String>> errors;
    private List<String> messages;
    private String type;
    private String name;
    private String rejectedValue;

    public static Error ofMessages(String central, List<String> messages) {
      return builder()
        .reason(INVALID_REQUEST_REASON)
        .central(central)
        .messages(messages)
        .build();
    }

    public static Error fieldError(String key, String rejectedValue) {
      return builder()
        .type(FIELD_ERROR_TYPE)
        .reason(INVALID_RECORD_KEY_REASON)
        .name(key)
        .rejectedValue(rejectedValue)
        .build();
    }
  }

}
