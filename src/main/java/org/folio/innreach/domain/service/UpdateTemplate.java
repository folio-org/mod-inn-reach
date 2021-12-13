package org.folio.innreach.domain.service;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface UpdateTemplate<K, R> {

  default Optional<R> update(K key, Finder<? super K, ? extends R> finder, UpdateOperation<R> updater) {
    return finder.apply(key).map(updater);
  }

  interface Finder<K, R> extends Function<K, Optional<R>> {

    default Finder<K, R> toRequired(Supplier<? extends RuntimeException> notFoundExceptionSupplier) {
      Objects.requireNonNull(notFoundExceptionSupplier);

      return key -> {
        var rec = this.apply(key);

        if (rec.isEmpty()) {
          throw notFoundExceptionSupplier.get();
        }

        return rec;
      };
    }

  }

  interface UpdateOperation<R> extends UnaryOperator<R> {

    default UpdateOperation<R> andThen(UnaryOperator<R> after) {
      Objects.requireNonNull(after);
      return (R rec) -> after.apply(apply(rec));
    }

  }

}
