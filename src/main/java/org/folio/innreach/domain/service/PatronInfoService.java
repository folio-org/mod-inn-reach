package org.folio.innreach.domain.service;

import org.folio.innreach.dto.PatronInfoResponseDTO;

public interface PatronInfoService {

  PatronInfoResponseDTO verifyPatron(String localServerCode, String visiblePatronId, String patronAgencyCode, String patronName);

}
