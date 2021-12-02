package org.folio.innreach.domain.service;

import java.util.Optional;

public interface BasicService<K, R> extends UpdateTemplateWithFinder<K, R> {

  R create(R aRecord);

  R update(R aRecord);

  Optional<R> find(K key);

  default Optional<R> changeAndUpdate(K key, UpdateOperation<R> change) {
    return update(key, change.andThen(this::update));
  }

  @Override
  default Finder<K, R> finder() {
    return this::find;
  }

}
