package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.experimental.UtilityClass;

import org.folio.innreach.domain.entity.CentralServer;

@UtilityClass
class ServiceUtils {

  static CentralServer centralServerRef(UUID centralServerId) {
    var server = new CentralServer();
    server.setId(centralServerId);

    return server;
  }
}