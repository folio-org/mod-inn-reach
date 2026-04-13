package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.function.Supplier;

public interface BasicService<K, R> extends UpdateTemplateWithFinder<K, R> {

  R create(R aRecord);

  R update(R aRecord);

  Optional<R> find(K key);

  void delete(K key);

  default Optional<R> changeAndUpdate(K key, UpdateOperation<R> change) {
    return update(key, change.andThen(this::update));
  }

  default Optional<R> changeAndUpdate(K key, Supplier<? extends RuntimeException> notFoundExceptionSupplier,
      UpdateOperation<R> change) {
    return update(key, finder().toRequired(notFoundExceptionSupplier), change.andThen(this::update));
  }

  @Override
  default Finder<K, R> finder() {
    return this::find;
  }

}
