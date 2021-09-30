package org.folio.innreach.external.service.impl;

import static java.util.List.of;

import static org.folio.innreach.external.dto.InnReachResponse.Error.fieldError;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.BibInfoResponseDTO;
import org.folio.innreach.external.dto.BibInfoResponse;
import org.folio.innreach.external.mapper.InnReachResponseMapper;
import org.folio.innreach.external.service.BibInfoService;

@Log4j2
@RequiredArgsConstructor
@Service
public class BibInfoServiceImpl implements BibInfoService {

  private static final String ERROR_MESSAGE = "Unknown centralCode and bibId combination";

  private final CentralServerService centralServerService;
  private final InventoryViewService instanceService;
  private final InstanceTransformationService instanceTransformationService;
  private final InnReachResponseMapper mapper;

  @Override
  public BibInfoResponseDTO getBibInfo(String bibId, String centralCode) {
    return mapper.toDto(fetchBibInfo(bibId, centralCode));
  }

  private BibInfoResponse fetchBibInfo(String bibId, String centralCode) {
    try {
      var centralServer = centralServerService.getCentralServerByCentralCode(centralCode);
      var instance = instanceService.getInstanceByHrid(bibId);
      var bib = instanceTransformationService.getBibInfo(centralServer.getId(), instance);

      return BibInfoResponse.ofBibInfo(bib);
    } catch (Exception e) {
      log.warn("Unable to load bib info by central code {} and inventory instance hrid {}", centralCode, bibId, e);

      return BibInfoResponse.errorResponse(ERROR_MESSAGE,
        of(fieldError("centralCode", centralCode), fieldError("bibId", bibId)));
    }
  }

}
