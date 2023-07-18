package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.service.TenantsHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantsHolderImpl implements TenantsHolder {

  List<String> tenants = new ArrayList<>();

  @Override
  public void add(String tenant) {
    tenants.add(tenant);
  }

  @Override
  public void remove(String tenant) {
    tenants.remove(tenant);
  }

  @Override
  public List<String> getAll() {
    return tenants;
  }
}
