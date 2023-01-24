package org.folio.innreach.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.List;

interface Condition<T extends Comparable<? super T>> {

  Predicate applyArguments(Expression<? extends T> exp, List<T> args);

  static <T extends Comparable<? super T>> Condition<T> positive(CriteriaBuilder cb) {
    return (exp, args) -> cb.conjunction();
  }

}
