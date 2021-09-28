package org.folio.innreach.external.service;

import org.folio.innreach.dto.BibInfoResponseDTO;

public interface BibInfoService {

  BibInfoResponseDTO getBibInfo(String bibId, String centralCode);

}
