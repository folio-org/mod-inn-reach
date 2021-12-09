package org.folio.innreach.domain.service;

import java.util.Optional;

public interface UpdateTemplateWithFinder<K, R> extends UpdateTemplate<K, R> {

  default Optional<R> update(K k, UpdateOperation<R> updater) {
    return update(k, finder(), updater);
  }

  Finder<K, R> finder();

}
