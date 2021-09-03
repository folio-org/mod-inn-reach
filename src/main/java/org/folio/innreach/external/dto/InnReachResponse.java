package org.folio.innreach.external.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class InnReachResponse {

  private String status;
  private String reason;
  private List<Error> errors;

  @Data
  @ToString
  public static class Error {
    private String central;
    private String reason;
    private List<String> messages;
  }

  public boolean isOk() {
    return "ok".equals(status) && CollectionUtils.isEmpty(errors);
  }

}
