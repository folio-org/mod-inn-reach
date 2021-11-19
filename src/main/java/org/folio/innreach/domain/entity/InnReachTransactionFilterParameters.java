package org.folio.innreach.domain.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
public class InnReachTransactionFilterParameters {
  private List<InnReachTransaction.TransactionType> types;
  private List<InnReachTransaction.TransactionState> states;
  private List<String> centralServerCodes;
  private List<String> patronAgencyCodes;
  private List<String> itemAgencyCodes;
  private List<Integer> patronTypes;
  private List<Integer> centralItemTypes;
  private String search;
  private SortBy sortBy;
  private SortOrder sortOrder;

  @AllArgsConstructor
  public enum SortBy{
    TRANSACTION_TIME("transactionTime"),
    DATE_CREATED("createdDate"),
    DATE_MODIFIED("updatedDate"),
    TRANSACTION_TYPE("type"),
    TRANSACTION_STATUS("state"),
    ITEM_AGENCY("itemAgencyCode"),
    PATRON_AGENCY("patronAgencyCode"),
    CENTRAL_PATRON_TYPE("centralPatronType"),
    CENTRAL_ITEM_TYPE("centralItemType");

    @Getter
    private final String value;
  }

  @AllArgsConstructor
  public enum SortOrder {
    ASC("asc"), DESC("desc");
    @Getter
    private final String value;
  }
}
