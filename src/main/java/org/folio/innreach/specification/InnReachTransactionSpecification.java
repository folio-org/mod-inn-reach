package org.folio.innreach.specification;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder.DESC;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.time.OffsetDateTime;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortBy;
import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters.SortOrder;
import org.folio.innreach.domain.entity.TransactionPatronHold;

@Component
public class InnReachTransactionSpecification {

  private static final String CREATED_DATE_FIELD = "createdDate";
  private static final String UPDATED_DATE_FIELD = "updatedDate";

  private static final String STATE = "state";

  public Specification<InnReachTransaction> filterByParameters(InnReachTransactionFilterParameters parameters) {
    return fetchHoldAndPickupLocation()
      .and(fieldsLookup(parameters))
      .and(keywordLookup(parameters.getQuery()))
      .and(sortBy(parameters.getSortBy(), parameters.getSortOrder()));
  }

  static Specification<InnReachTransaction> fieldsLookup(InnReachTransactionFilterParameters parameters) {
    return (transaction, cq, cb) -> {
      var isRequestTooLongReport = parameters.isRequestedTooLong();
      var hold = transaction.join("hold");
      var patronHold = cb.treat(hold, TransactionPatronHold.class);

      var typeIs = isOfType(cb, transaction, parameters);
      var stateIs = isOfState(cb, transaction, parameters);
      var centralCodeIn = centralCodeIn(cb, transaction, parameters);
      var patronAgencyIn = patronAgencyIn(cb, hold, parameters);
      var itemAgencyIn = itemAgencyIn(cb, hold, parameters);
      var patronTypeIn = patronTypeIn(cb, hold, parameters);
      var patronNameIn = patronNameIn(cb, hold, parameters);
      var centralItemTypeIn = centralItemTypeIn(cb, hold, parameters);
      var itemBarcodeIn = itemBarcodeIn(cb, transaction, hold, patronHold, parameters);

      var odtConditionFactory = new ConditionFactory<OffsetDateTime>(cb);

      var createdDateIs = odtConditionFactory.create(parameters.getCreatedDateOperation())
          .applyArguments(transaction.get(CREATED_DATE_FIELD), parameters.getCreatedDates());
      var updatedDateIs = odtConditionFactory.create(parameters.getUpdatedDateOperation())
          .applyArguments(transaction.get(UPDATED_DATE_FIELD), parameters.getUpdatedDates());
      var holdCreatedDateIs = odtConditionFactory.create(parameters.getHoldCreatedDateOperation())
          .applyArguments(hold.get(CREATED_DATE_FIELD), parameters.getHoldCreatedDates());
      var holdUpdatedDateIs = odtConditionFactory.create(parameters.getHoldUpdatedDateOperation())
          .applyArguments(hold.get(UPDATED_DATE_FIELD), parameters.getHoldUpdatedDates());

      var dueDatesAsEpochSecs = mapItems(parameters.getDueDates(), odt -> (int) odt.toEpochSecond());
      var dueDateIs = new ConditionFactory<Integer>(cb).create(parameters.getDueDateOperation())
          .applyArguments(hold.get("dueDateTime"), dueDatesAsEpochSecs);

      if(!isRequestTooLongReport) {
        return cb.and(typeIs, stateIs, centralCodeIn, patronAgencyIn, itemAgencyIn, patronTypeIn, centralItemTypeIn,
          itemBarcodeIn, patronNameIn, createdDateIs, updatedDateIs, holdCreatedDateIs, holdUpdatedDateIs, dueDateIs);
      } else {
        var patronStateWithOrDateCondition = createCriteriaForRequestTooLongReport(cb,transaction,createdDateIs,updatedDateIs);
        return cb.and(typeIs, stateIs, centralCodeIn, patronAgencyIn, itemAgencyIn, patronTypeIn, centralItemTypeIn,
          itemBarcodeIn, patronNameIn, patronStateWithOrDateCondition, holdCreatedDateIs, holdUpdatedDateIs, dueDateIs);
      }
    };
  }

  private static Predicate createCriteriaForRequestTooLongReport(CriteriaBuilder cb, Root<InnReachTransaction> transaction, Predicate createdDateIs, Predicate updatedDateIs) {
      var holdState = cb.equal(transaction.get(STATE), PATRON_HOLD);
      var holdStateAndCreateDate = cb.and(holdState,createdDateIs);
      var transferState = cb.equal(transaction.get(STATE), TRANSFER);
      var transferStateAndUpdatedDate = cb.and(transferState, updatedDateIs);
      return cb.or(holdStateAndCreateDate, transferStateAndUpdatedDate);
  }

  static Specification<InnReachTransaction> keywordLookup(String keyword) {
    return (transaction, cq, cb) -> {
      if (StringUtils.isBlank(keyword)) {
        return cb.conjunction();
      }
      var lowerCaseKeyword = keyword.toLowerCase();

      var hold = transaction.join("hold");

      var itemIdMatch = cb.equal(hold.get("itemId"), keyword);
      var patronIdMatch = cb.equal(hold.get("patronId"), keyword);
      var trackingIdMatch = cb.equal(transaction.get("trackingId"), keyword);
      var patronBarcodeMatch = cb.equal(hold.get("folioPatronBarcode"), keyword);
      var itemBarcodeMatch = cb.equal(hold.get("folioItemBarcode"), keyword);
      var itemAuthorLike = cb.like(cb.lower(hold.get("author")), "%" + lowerCaseKeyword + "%");
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

  static Predicate isOfType(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var types = parameters.getTypes();
    return isEmpty(types) ? cb.conjunction() : transaction.get("type").in(types);
  }

  static Predicate isOfState(CriteriaBuilder cb, Root<InnReachTransaction> transaction,
      InnReachTransactionFilterParameters parameters) {
    var states = parameters.getStates();
    return isEmpty(states) ? cb.conjunction() : transaction.get(STATE).in(states);
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

  static Predicate patronTypeIn(CriteriaBuilder cb, Join<Object, Object>  transactionHold, InnReachTransactionFilterParameters parameters) {
    var patronTypes = parameters.getPatronTypes();
    if (isEmpty(patronTypes)) {
      return cb.conjunction();
    }

    return transactionHold.get("centralPatronType").in(patronTypes);
  }

  static Predicate patronNameIn(CriteriaBuilder cb, Join<Object, Object>  transactionHold, InnReachTransactionFilterParameters parameters) {
    var patronNames = parameters.getPatronNames();

    return isEmpty(patronNames) ? cb.conjunction() : transactionHold.get("patronName").in(patronNames);
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
        var order = sortOrder == DESC ? cb.desc(getField(transaction, sortBy)) : cb.asc(getField(transaction, sortBy));
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
