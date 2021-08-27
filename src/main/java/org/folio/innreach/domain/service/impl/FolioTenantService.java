package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class FolioTenantService {

  private final SystemUserService systemUserService;
  private final KafkaService kafkaService;

  public void initializeTenant() {
    systemUserService.prepareSystemUser();
    kafkaService.restartEventListeners();
  }

}
