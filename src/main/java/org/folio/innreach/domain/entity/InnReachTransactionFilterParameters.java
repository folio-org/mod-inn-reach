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
  private TransactionSortBy sortBy;
  private SortOrder sortOrder;
  private String itemBarcode;
  private String patronBarcode;
  private String itemTitle;
  private String itemAuthor;
  private String trackingId;
  private String patronId;
  private String itemId;

  @AllArgsConstructor
  public enum TransactionSortBy {
    TRANSACTION_TIME("transactionTime"),
    DATE_CREATED("createdDate"),
    DATE_MODIFIED("updatedDate"),
    TRANSACTION_TYPE("type"),
    TRANSACTION_STATUS("state"),
    ITEM_AGENCY("itemAgencyCode"),
    PATRON_AGENCY("patronAgencyCode"),
    PATRON_TYPE("centralPatronType"),
    CENTRAL_ITEM_TYPE("centralItemType");

    @Getter
    private final String value;
  }

  public enum SortOrder { ASC, DESC }
}
