package org.folio.innreach.external.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.folio.innreach.dto.PatronInfo;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PatronInfoResponse extends InnReachResponse {

  private PatronInfo patronInfo;
  private Boolean requestAllowed;

  public static PatronInfoResponse of(PatronInfo patronInfo, Boolean requestAllowed) {
    return PatronInfoResponse.builder()
      .status(OK_STATUS)
      .reason(OK_REASON)
      .patronInfo(requestAllowed ? patronInfo : null)
      .requestAllowed(requestAllowed)
      .build();
  }

  public static PatronInfoResponse error(String reason, Error error) {
    return PatronInfoResponse.builder()
      .reason(reason)
      .status(ERROR_STATUS)
      .errors(List.of(error))
      .requestAllowed(false)
      .build();
  }

}
