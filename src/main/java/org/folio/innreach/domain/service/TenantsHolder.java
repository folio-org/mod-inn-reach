package org.folio.innreach.domain.service;

import java.util.List;

public interface TenantsHolder {

  void add(String tenant);

  void remove(String tenant);

  List<String> getAll();

}
