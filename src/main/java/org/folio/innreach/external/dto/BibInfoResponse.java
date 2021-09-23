package org.folio.innreach.external.dto;

import static java.util.List.of;

import static org.folio.innreach.external.dto.InnReachResponse.Error.fieldError;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.folio.innreach.dto.BibInfo;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BibInfoResponse extends InnReachResponse {

  private BibInfo bibInfo;

  public static BibInfoResponse ofBibInfo(BibInfo bibInfo) {
    return BibInfoResponse.builder()
      .status(OK_STATUS)
      .reason(OK_REASON)
      .bibInfo(bibInfo)
      .build();
  }

  public static BibInfoResponse errorResponse(String reason, List<Error> errors) {
    return BibInfoResponse.builder()
      .reason(reason)
      .status(ERROR_STATUS)
      .errors(errors)
      .build();
  }

}
