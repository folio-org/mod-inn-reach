package org.folio.innreach.external.service;

import java.util.UUID;

public interface InnReachExternalService {

  String callInnReachApi(UUID centralServerId, String innReachRequestUri);

  String postInnReachApi(String centralCode, String innReachRequestUri, Object payload);

  String postInnReachApi(String centralCode, String innReachRequestUri);
}
