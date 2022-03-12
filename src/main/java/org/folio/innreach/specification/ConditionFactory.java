package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.BETWEEN;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.GREATER;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.GREATER_OR_EQUAL;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.LESS;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.LESS_OR_EQUAL;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.DateOperation.NOT_EQUAL;
import static org.folio.innreach.specification.Condition.positive;

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
    return (dateField, args) -> isEmpty(args)
        ? cb.conjunction()
        : condition(operation).applyArguments(dateField, args);
  }

  private Condition<T> condition(InnReachTransactionFilterParameters.DateOperation operation) {
    return operation == null
        ? positive(cb)
        : operationToCondition.getOrDefault(operation, equal());
  }

  private Condition<T> less() {
    return (dateField, args) -> cb.lessThan(dateField, args.get(0));
  }

  private Condition<T> lessOrEqual() {
    return (dateField, args) -> cb.lessThanOrEqualTo(dateField, args.get(0));
  }

  private Condition<T> equal() {
    return (dateField, args) -> cb.equal(dateField, args.get(0));
  }

  private Condition<T> notEqual() {
    return (dateField, args) -> cb.notEqual(dateField, args.get(0));
  }

  private Condition<T> greater() {
    return (dateField, args) -> cb.greaterThan(dateField, args.get(0));
  }

  private Condition<T> greaterOrEqual() {
    return (dateField, args) -> cb.greaterThanOrEqualTo(dateField, args.get(0));
  }

  private Condition<T> between() {
    return (dateField, args) -> cb.between(dateField, args.get(0), args.get(1));
  }

}