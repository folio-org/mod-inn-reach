package org.folio.innreach.specification;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

interface Condition<T extends Comparable<? super T>> {

  Predicate applyArguments(Expression<? extends T> exp, List<T> args);

  static <T extends Comparable<? super T>> Condition<T> positive(CriteriaBuilder cb) {
    return (exp, args) -> cb.conjunction();
  }

}