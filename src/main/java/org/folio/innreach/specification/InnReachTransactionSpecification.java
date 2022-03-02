package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder.DESC;

import java.time.OffsetDateTime;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortBy;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;

@Component
public class InnReachTransactionSpecification {

  public Specification<InnReachTransaction> filterByParameters(InnReachTransactionFilterParameters parameters) {
    return fetchHoldAndPickupLocation()
      .and(fieldsLookup(parameters))
      .and(keywordLookup(parameters.getQuery()))
      .and(sortBy(parameters.getSortBy(), parameters.getSortOrder()));
  }

  static Specification<InnReachTransaction> fieldsLookup(InnReachTransactionFilterParameters parameters) {
    return (transaction, cq, cb) -> {
      var hold = transaction.join("hold");
      var itemHold = cb.treat(hold, TransactionItemHold.class);
      var localHold = cb.treat(hold, TransactionLocalHold.class);
      var patronHold = cb.treat(hold, TransactionPatronHold.class);

      var typeIs = isOfType(cb, transaction, parameters);
      var stateIs = isOfState(cb, transaction, parameters);
      var centralCodeIn = centralCodeIn(cb, transaction, parameters);
      var patronAgencyIn = patronAgencyIn(cb, hold, parameters);
      var itemAgencyIn = itemAgencyIn(cb, hold, parameters);
      var patronTypeIn = patronTypeIn(cb, itemHold, localHold, parameters);
      var centralItemTypeIn = centralItemTypeIn(cb, hold, parameters);
      var itemBarcodeIn = itemBarcodeIn(cb, transaction, hold, patronHold, parameters);
      var createDateIs = createDateIs(cb, transaction, parameters);

      return cb.and(typeIs, stateIs, centralCodeIn, patronAgencyIn, itemAgencyIn, patronTypeIn, centralItemTypeIn,
          itemBarcodeIn, createDateIs);
    };
  }

  static Specification<InnReachTransaction> keywordLookup(String keyword) {
    return (transaction, cq, cb) -> {
      if (StringUtils.isBlank(keyword)) {
        return cb.conjunction();
      }
      var lowerCaseKeyword = keyword.toLowerCase();

      var hold = transaction.join("hold");
      var localHold = cb.treat(hold, TransactionLocalHold.class);
      var patronHold = cb.treat(hold, TransactionPatronHold.class);

      var itemIdMatch = cb.equal(hold.get("itemId"), keyword);
      var patronIdMatch = cb.equal(hold.get("patronId"), keyword);
      var trackingIdMatch = cb.equal(transaction.get("trackingId"), keyword);
      var patronBarcodeMatch = cb.equal(hold.get("folioPatronBarcode"), keyword);
      var itemBarcodeMatch = cb.equal(hold.get("folioItemBarcode"), keyword);
      var itemAuthorLike = cb.or(
        cb.like(cb.lower(localHold.get("authorLocal")), "%" + lowerCaseKeyword + "%"),
        cb.like(cb.lower(patronHold.get("authorPatron")), "%" + lowerCaseKeyword + "%"));
      var itemTitleLike = cb.like(cb.lower(hold.get("title")), "%" + lowerCaseKeyword + "%");

      return cb.or(itemIdMatch, patronIdMatch, trackingIdMatch, patronBarcodeMatch, itemBarcodeMatch, itemAuthorLike, itemTitleLike);
    };
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

  private static Predicate createDateIs(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var dates = parameters.getCreateDates();
    return isEmpty(dates) ? cb.conjunction() : buildDateCondition(cb, transaction.get("createdDate"), 
        dates, parameters.getCreatedDateOperation());
  }

  private static Predicate buildDateCondition(CriteriaBuilder cb, Path<OffsetDateTime> date,
      List<OffsetDateTime> dateValues, InnReachTransactionFilterParameters.DateOperation operation) {
    assert isNotEmpty(dateValues);

    var firstValue = dateValues.get(0);
    var secondValue = dateValues.size() == 2 ? dateValues.get(1) : null;

    Predicate result;
    switch (operation) {
      case LESS:
        result = cb.lessThan(date, firstValue);
        break;
      case LESS_OR_EQUAL:
        result = cb.lessThanOrEqualTo(date, firstValue);
        break;
      case NOT_EQUAL:
        result = cb.notEqual(date, firstValue);
        break;
      case GREATER:
        result = cb.greaterThan(date, firstValue);
        break;
      case GREATER_OR_EQUAL:
        result = cb.greaterThanOrEqualTo(date, firstValue);
        break;
      case BETWEEN:
        result = cb.between(date, firstValue, secondValue);
        break;
      case EQUAL:
      default:
        result = cb.equal(date, firstValue);
    }

    return result;
  }

  static Predicate isOfType(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var types = parameters.getTypes();
    return isEmpty(types) ? cb.conjunction() : transaction.get("type").in(types);
  }

  static Predicate isOfState(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var states = parameters.getStates();
    return isEmpty(states) ? cb.conjunction() : transaction.get("state").in(states);
  }

  static Predicate centralCodeIn(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var centralCodes = parameters.getCentralServerCodes();
    return isEmpty(centralCodes) ? cb.conjunction() : transaction.get("centralServerCode").in(centralCodes);
  }

  static Predicate patronAgencyIn(CriteriaBuilder cb, Join<Object, Object> hold,
      InnReachTransactionFilterParameters parameters) {
    var patronAgencies = parameters.getPatronAgencyCodes();
    return isEmpty(patronAgencies) ? cb.conjunction() : hold.get("patronAgencyCode").in(patronAgencies);
  }

  static Predicate itemAgencyIn(CriteriaBuilder cb, Join<Object, Object> hold,
      InnReachTransactionFilterParameters parameters) {
    var itemAgencies = parameters.getItemAgencyCodes();
    return isEmpty(itemAgencies) ? cb.conjunction() : hold.get("itemAgencyCode").in(itemAgencies);
  }

  static Predicate patronTypeIn(CriteriaBuilder cb,
                                Join<Object, TransactionItemHold> itemHold,
                                Join<Object, TransactionLocalHold> localHold,
                                InnReachTransactionFilterParameters parameters) {
    var patronTypes = parameters.getPatronTypes();
    if (isEmpty(patronTypes)) {
      return cb.conjunction();
    }

    return cb.or(
      itemHold.get("centralPatronTypeItem").in(patronTypes),
      localHold.get("centralPatronTypeLocal").in(patronTypes));
  }

  static Predicate itemBarcodeIn(CriteriaBuilder cb,
                                 Root<InnReachTransaction> transaction,
                                 Join<Object, Object> hold,
                                 Join<Object, TransactionPatronHold> patronHold,
                                 InnReachTransactionFilterParameters parameters) {
    var itemBarcodes = parameters.getItemBarcodes();
    if (isEmpty(itemBarcodes)) {
      return cb.conjunction();
    }

    var shippedItemBarcodePredicate = cb.and(
      cb.equal(transaction.get("type"), PATRON),
      patronHold.get("shippedItemBarcode").in(itemBarcodes)
    );

    var folioItemBarcodePredicate = cb.and(
      cb.notEqual(transaction.get("type"), PATRON),
      hold.get("folioItemBarcode").in(itemBarcodes)
    );

    return cb.or(shippedItemBarcodePredicate, folioItemBarcodePredicate);
  }

  static Predicate centralItemTypeIn(CriteriaBuilder cb, Join<Object, Object> hold,
      InnReachTransactionFilterParameters parameters) {
    var centralItemTypes = parameters.getCentralItemTypes();
    return isEmpty(centralItemTypes) ? cb.conjunction() : hold.get("centralItemType").in(centralItemTypes);
  }

  static Specification<InnReachTransaction> sortBy(SortBy sortBy, SortOrder sortOrder) {
    return (transaction, cq, cb) -> {
      if (sortBy != null) {
        Order order;
        if (sortBy == SortBy.CENTRAL_PATRON_TYPE) {
          var join = transaction.join("hold");
          var itemHold = cb.treat(join, TransactionItemHold.class);
          var localHold = cb.treat(join, TransactionLocalHold.class);

          var coalesce = cb.coalesce(itemHold.get("centralPatronTypeItem"), localHold.get("centralPatronTypeLocal"));
          order = sortOrder == DESC ? cb.desc(coalesce) : cb.asc(coalesce);
        } else {
          order = sortOrder == DESC ? cb.desc(getField(transaction, sortBy)) : cb.asc(getField(transaction, sortBy));
        }
        cq.orderBy(order);
      }
      return cb.conjunction();
    };
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
