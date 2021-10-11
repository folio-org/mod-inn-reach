package org.folio.innreach.external.service;

import java.net.URI;
import java.util.UUID;

public interface InnReachExternalService {

  String callInnReachApi(UUID centralServerId, String innReachRequestUri);

  String postInnReachApi(UUID centralServerId, URI innReachRequestUri, Object dto);
}
