package org.folio.innreach.external.service;

import java.util.UUID;

public interface InnReachExternalService {

  String callInnReachApi(UUID centralServerId, String innReachRequestUri);
}
