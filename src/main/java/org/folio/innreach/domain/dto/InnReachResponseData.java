package org.folio.innreach.domain.dto;

import java.util.List;

import org.folio.innreach.dto.InnReachError;

public interface InnReachResponseData {
  String getStatus();

  String getReason();

  List<InnReachError> getErrors();
}
