package org.folio.innreach.domain.entity;

import java.time.OffsetDateTime;
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
  private List<String> itemBarcodes;
  private List<Integer> patronTypes;
  private List<Integer> centralItemTypes;
  private List<String> patronNames;
  private String query;
  private List<OffsetDateTime> createdDates;
  private DateOperation createdDateOperation;
  private List<OffsetDateTime> updatedDates;
  private DateOperation updatedDateOperation;
  private List<OffsetDateTime> holdCreatedDates;
  private DateOperation holdCreatedDateOperation;
  private List<OffsetDateTime> holdUpdatedDates;
  private DateOperation holdUpdatedDateOperation;
  private List<OffsetDateTime> dueDates;
  private DateOperation dueDateOperation;
  private SortBy sortBy;
  private SortOrder sortOrder;

  @AllArgsConstructor
  public enum SortBy {
    TRANSACTION_TIME("transactionTime"),
    DATE_CREATED("createdDate"),
    DATE_MODIFIED("updatedDate"),
    TRANSACTION_TYPE("type"),
    TRANSACTION_STATUS("state"),
    ITEM_AGENCY("itemAgencyCode"),
    PATRON_AGENCY("patronAgencyCode"),
    CENTRAL_PATRON_TYPE("centralPatronType"),
    CENTRAL_ITEM_TYPE("centralItemType"),
    PATRON_NAME("patronName");

    @Getter
    private final String value;
  }

  @AllArgsConstructor
  public enum SortOrder {
    ASC("asc"), DESC("desc");
    @Getter
    private final String value;
  }

  @AllArgsConstructor
  public enum DateOperation {

    LESS("less"),
    LESS_OR_EQUAL("lessOrEqual"),
    EQUAL("equal"),
    NOT_EQUAL("notEqual"),
    GREATER("greater"),
    GREATER_OR_EQUAL("greaterOrEqual"),
    BETWEEN("between");

    @Getter
    private final String value;
  }

}
