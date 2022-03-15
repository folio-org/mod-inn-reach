package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.BETWEEN;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.GREATER;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.GREATER_OR_EQUAL;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.LESS;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.LESS_OR_EQUAL;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.NOT_EQUAL;

import java.util.EnumMap;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;

import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;

class ConditionFactory<T extends Comparable<? super T>> {

  private final Map<InnReachTransactionFilterParameters.DateOperation, Condition<T>> operationToCondition =
      new EnumMap<>(InnReachTransactionFilterParameters.DateOperation.class);
  private final CriteriaBuilder cb;


  ConditionFactory(CriteriaBuilder criteriaBuilder) {
    this.cb = criteriaBuilder;

    operationToCondition.put(LESS, less());
    operationToCondition.put(LESS_OR_EQUAL, lessOrEqual());
    operationToCondition.put(NOT_EQUAL, notEqual());
    operationToCondition.put(GREATER, greater());
    operationToCondition.put(GREATER_OR_EQUAL, greaterOrEqual());
    operationToCondition.put(BETWEEN, between());
  }

  Condition<T> create(InnReachTransactionFilterParameters.DateOperation operation) {
    return (exp, args) -> isEmpty(args)
        ? cb.conjunction()
        : operationToCondition.getOrDefault(operation, equal()).applyArguments(exp, args);
  }

  private Condition<T> less() {
    return (exp, args) -> cb.lessThan(exp, args.get(0));
  }

  private Condition<T> lessOrEqual() {
    return (exp, args) -> cb.lessThanOrEqualTo(exp, args.get(0));
  }

  @SuppressWarnings("java:S1221")
  private Condition<T> equal() {
    return (exp, args) -> cb.equal(exp, args.get(0));
  }

  private Condition<T> notEqual() {
    return (exp, args) -> cb.notEqual(exp, args.get(0));
  }

  private Condition<T> greater() {
    return (exp, args) -> cb.greaterThan(exp, args.get(0));
  }

  private Condition<T> greaterOrEqual() {
    return (exp, args) -> cb.greaterThanOrEqualTo(exp, args.get(0));
  }

  private Condition<T> between() {
    return (exp, args) -> cb.between(exp, args.get(0), args.get(1));
  }

}