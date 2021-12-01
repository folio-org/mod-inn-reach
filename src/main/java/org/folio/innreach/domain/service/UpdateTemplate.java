package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.folio.innreach.domain.exception.EntityNotFoundException;

public interface UpdateTemplate<Key, Rec> {

  default Rec update(Key k, Finder<Key, Rec> finder, UpdateOperation<Rec> updater) {
    return finder.apply(k)
        .map(updater)
        .orElseThrow(() -> notFoundExceptionProducer(k));
  }

  default EntityNotFoundException notFoundExceptionProducer(Key k) {
    return new EntityNotFoundException("Record is not found: key = " + k);
  }

  interface Finder<Key, Rec> extends Function<Key, Optional<Rec>> {}

  interface UpdateOperation<Rec> extends UnaryOperator<Rec> {}

}
