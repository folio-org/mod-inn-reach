package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder.DESC;

import java.util.List;

import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortBy;
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
      .and(sortBy(parameters.getSortBy(), parameters.getSortOrder()))
      .and(itemBarcodeMatch(parameters.getItemBarcode()))
      .and(patronBarcodeMatch(parameters.getPatronBarcode()))
      .and(itemTitleLike(parameters.getItemTitle()))
      .and(itemAuthorLike(parameters.getItemAuthor()))
      .and(trackingIdMatch(parameters.getTrackingId()))
      .and(itemIdMatch(parameters.getItemId()))
      .and(patronIdMatch(parameters.getPatronId()));
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
      var join = transaction.join("hold");
      var itemHold = cb.treat(join, TransactionItemHold.class);
      var localHold = cb.treat(join, TransactionLocalHold.class);
      return cb.or(
        itemHold.get("centralPatronTypeItem").in(patronTypes),
        localHold.get("centralPatronTypeLocal").in(patronTypes));
    };
  }

  static Specification<InnReachTransaction> centralItemTypeIn(List<Integer> centralItemTypes) {
    return (transaction, cq, cb) -> isEmpty(centralItemTypes) ? cb.conjunction() : transaction.get("hold").get("centralItemType").in(centralItemTypes);
  }

  static Specification<InnReachTransaction> sortBy(SortBy sortBy, SortOrder sortOrder) {
    return (transaction, cq, cb) -> {
      if (sortBy != null) {
        Order order;
        if (sortBy == SortBy.CENTRAL_PATRON_TYPE) {
          var join = transaction.join("hold");
          var itemHold = cb.treat(join, TransactionItemHold.class);
          var localHold = cb.treat(join, TransactionLocalHold.class);

          var coalesce = cb.coalesce(itemHold.get("centralPatronTypeItem"),
            localHold.get("centralPatronTypeLocal"));
          order = sortOrder == DESC ? cb.desc(coalesce) : cb.asc(coalesce);
        } else {
          order = sortOrder == DESC ? cb.desc(getField(transaction, sortBy)) : cb.asc(getField(transaction, sortBy));
        }
        cq.orderBy(order);
      }
      return cb.conjunction();
    };
  }

  static Specification<InnReachTransaction> itemBarcodeMatch(String itemBarcode) {
    return (transaction, cq, cb) -> itemBarcode == null ? cb.conjunction() :
      cb.equal(transaction.get("hold").get("folioItemBarcode"), itemBarcode);
  }

  static Specification<InnReachTransaction> patronBarcodeMatch(String patronBarcode) {
    return (transaction, cq, cb) -> patronBarcode == null ? cb.conjunction() :
      cb.equal(transaction.get("hold").get("folioPatronBarcode"), patronBarcode);
  }

  static Specification<InnReachTransaction> itemTitleLike(String itemTitle) {
    return (transaction, cq, cb) -> {
      if (itemTitle == null) {
        return cb.conjunction();
      }
      else {
        var join = transaction.join("hold");
        var localHold = cb.treat(join, TransactionLocalHold.class);
        var patronHold = cb.treat(join, TransactionPatronHold.class);
        return cb.or(
          cb.like(cb.lower(localHold.get("titleLocal")), "%" + itemTitle.toLowerCase() + "%"),
          cb.like(cb.lower(patronHold.get("titlePatron")), "%" + itemTitle.toLowerCase() + "%"));
      }
    };
  }

  static Specification<InnReachTransaction> itemAuthorLike(String itemAuthor) {
    return (transaction, cq, cb) -> {
      if (itemAuthor == null) {
        return cb.conjunction();
      }
      else {
        var join = transaction.join("hold");
        var localHold = cb.treat(join, TransactionLocalHold.class);
        var patronHold = cb.treat(join, TransactionPatronHold.class);
        return cb.or(
          cb.like(cb.lower(localHold.get("authorLocal")), "%" + itemAuthor.toLowerCase() + "%"),
          cb.like(cb.lower(patronHold.get("authorPatron")), "%" + itemAuthor.toLowerCase() + "%"));
      }
    };
  }

  static Specification<InnReachTransaction> trackingIdMatch(String trackingId) {
    return (transaction, cq, cb) -> trackingId == null ? cb.conjunction() :
      cb.equal(transaction.get("trackingId"), trackingId);
  }

  static Specification<InnReachTransaction> patronIdMatch(String patronId) {
    return (transaction, cq, cb) -> patronId == null ? cb.conjunction() :
      cb.equal(transaction.get("hold").get("patronId"), patronId);
  }

  static Specification<InnReachTransaction> itemIdMatch(String itemId) {
    return (transaction, cq, cb) -> itemId == null ? cb.conjunction() :
      cb.equal(transaction.get("hold").get("itemId"), itemId);
  }

  private static Expression<InnReachTransaction> getField(Root<InnReachTransaction> root, SortBy sort) {
    Expression<InnReachTransaction> expression;
    switch (sort) {
      case TRANSACTION_TYPE:
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
