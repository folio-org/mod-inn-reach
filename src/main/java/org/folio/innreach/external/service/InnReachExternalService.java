package org.folio.innreach.external.service;

import java.net.URI;
import java.util.UUID;

public interface InnReachExternalService {

  String callInnReachApi(UUID centralServerId, String innReachRequestUri);

  String postInnReachApi(UUID centralServerId, URI innReachRequestUri, Object dto);

  String postInnReachApi(String centralCode, String innReachRequestUri, Object payload);

  String postInnReachApi(String centralCode, String innReachRequestUri);
}
