package org.folio.innreach.specification;

import org.folio.innreach.domain.entity.InnReachTransactionSortingParameters;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;

@Component
public class InnReachTransactionSpecification {

  public Specification<InnReachTransaction> filterByParameters(InnReachTransactionSortingParameters parameters) {
    return fetchHoldAndPickupLocation()
      .and(isOfType(parameters.getTypes()))
      .and(isOfState(parameters.getStates()))
      .and(centralCodeIn(parameters.getCentralServerCodes()))
      .and(patronAgencyIn(parameters.getPatronAgencyCodes()))
      .and(itemAgencyIn(parameters.getItemAgencyCodes()))
      .and(patronTypeIn(parameters.getPatronTypes()))
      .and(centralItemTypeIn(parameters.getCentralItemTypes()));
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
    return (transaction, cq, cb) -> listIsNullOrEmpty(types) ? cb.conjunction() : transaction.get("type").in(types);
  }

  static Specification<InnReachTransaction> isOfState(List<InnReachTransaction.TransactionState> states) {
    return (transaction, cq, cb) -> listIsNullOrEmpty(states) ? cb.conjunction() : transaction.get("state").in(states);
  }

  static Specification<InnReachTransaction> centralCodeIn(List<String> centralCodes) {
    return (transaction, cq, cb) -> listIsNullOrEmpty(centralCodes) ? cb.conjunction() : transaction.get("centralServerCode").in(centralCodes);
  }

  static Specification<InnReachTransaction> patronAgencyIn(List<String> patronAgencies) {
    return (transaction, cq, cb) -> listIsNullOrEmpty(patronAgencies) ? cb.conjunction() : transaction.get("hold").get("patronAgencyCode").in(patronAgencies);
  }

  static Specification<InnReachTransaction> itemAgencyIn(List<String> itemAgencies) {
    return (transaction, cq, cb) -> listIsNullOrEmpty(itemAgencies) ? cb.conjunction() : transaction.get("hold").get("itemAgencyCode").in(itemAgencies);
  }

  static Specification<InnReachTransaction> patronTypeIn(List<Integer> patronTypes) {
    return (transaction, cq, cb) -> {
      if (patronTypes == null || patronTypes.isEmpty()) {
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
    return (transaction, cq, cb) -> listIsNullOrEmpty(centralItemTypes) ? cb.conjunction() : transaction.get("hold").get("centralItemType").in(centralItemTypes);
  }

  private static boolean listIsNullOrEmpty(List<?> list){
    return list == null || list.isEmpty();
  }
}
