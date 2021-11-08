package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder.DESC;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.TransactionSortBy;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

@Component
public class InnReachTransactionSpecification {

  public Specification<InnReachTransaction> filterByParameters(InnReachTransactionFilterParameters parameters) {
    return fetchHoldAndPickupLocation()
      .and(isOfType(parameters.getTypes()))
      .and(isOfState(parameters.getStates()))
      .and(centralCodeIn(parameters.getCentralServerCodes()))
      .and(patronAgencyIn(parameters.getPatronAgencyCodes()))
      .and(itemAgencyIn(parameters.getItemAgencyCodes()))
      .and(patronTypeIn(parameters.getPatronTypes()))
      .and(centralItemTypeIn(parameters.getCentralItemTypes()))
      .and(sortBy(parameters.getSortBy(), parameters.getSortOrder()));
  }

  static Specification<InnReachTransaction> fetchHoldAndPickupLocation() {
    return (transaction, cq, cb) -> {
      if (Long.class != cq.getResultType()) {
        var hold = transaction.fetch("hold");
        hold.fetch("pickupLocation");
      }
      return cb.conjunction();
    };
  }

  static Specification<InnReachTransaction> isOfType(List<InnReachTransaction.TransactionType> types) {
    return (transaction, cq, cb) -> isEmpty(types) ? cb.conjunction() : transaction.get("type").in(types);
  }

  static Specification<InnReachTransaction> isOfState(List<InnReachTransaction.TransactionState> states) {
    return (transaction, cq, cb) -> isEmpty(states) ? cb.conjunction() : transaction.get("state").in(states);
  }

  static Specification<InnReachTransaction> centralCodeIn(List<String> centralCodes) {
    return (transaction, cq, cb) -> isEmpty(centralCodes) ? cb.conjunction() : transaction.get("centralServerCode").in(centralCodes);
  }

  static Specification<InnReachTransaction> patronAgencyIn(List<String> patronAgencies) {
    return (transaction, cq, cb) -> isEmpty(patronAgencies) ? cb.conjunction() : transaction.get("hold").get("patronAgencyCode").in(patronAgencies);
  }

  static Specification<InnReachTransaction> itemAgencyIn(List<String> itemAgencies) {
    return (transaction, cq, cb) -> isEmpty(itemAgencies) ? cb.conjunction() : transaction.get("hold").get("itemAgencyCode").in(itemAgencies);
  }

  static Specification<InnReachTransaction> patronTypeIn(List<Integer> patronTypes) {
    return (transaction, cq, cb) -> {
      if (isEmpty(patronTypes)) {
        return cb.conjunction();
      }
      var itemHold = cb.treat(transaction.join("hold"), TransactionItemHold.class);
      var localHold = cb.treat(transaction.join("hold"), TransactionLocalHold.class);
      return cb.or(
        cb.and(cb.equal(transaction.get("type"), ITEM), itemHold.get("centralPatronType").in(patronTypes)),
        cb.and(cb.equal(transaction.get("type"), LOCAL), localHold.get("centralPatronType").in(patronTypes))
      );
    };
  }

  static Specification<InnReachTransaction> centralItemTypeIn(List<Integer> centralItemTypes) {
    return (transaction, cq, cb) -> isEmpty(centralItemTypes) ? cb.conjunction() : transaction.get("hold").get("centralItemType").in(centralItemTypes);
  }

  static Specification<InnReachTransaction> sortBy(List<TransactionSortBy> sortBy, SortOrder sortOrder) {
    return (transaction, cq, cb) -> {
      if (!isEmpty(sortBy)) {
        var orderList = new ArrayList<Order>();
        if (sortOrder == DESC) {
          sortBy.forEach(sort -> orderList.add(cb.desc(getField(transaction, sort))));
        } else {
          sortBy.forEach(sort -> orderList.add(cb.asc(getField(transaction, sort))));
        }
        cq.orderBy(orderList);
      }
      return cb.conjunction();
    };
  }

  private static Expression<InnReachTransaction> getField(Root<InnReachTransaction> root, TransactionSortBy sort) {
    Expression<InnReachTransaction> expression;
    switch (sort) {
      case TRANSACTION_TIME:
      case TRANSACTION_STATUS:
      case DATE_CREATED:
      case DATE_MODIFIED:
        expression = root.get(sort.getValue());
        break;
      default:
        expression = root.get("hold").get(sort.getValue());
    }
    return expression;
  }
}
