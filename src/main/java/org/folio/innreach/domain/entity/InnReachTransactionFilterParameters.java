package org.folio.innreach.domain.entity;

import java.util.List;

import lombok.Data;

@Data
public class InnReachTransactionFilterParameters {
  private List<InnReachTransaction.TransactionType> types;
  private List<InnReachTransaction.TransactionState> states;
  private List<String> centralServerCodes;
  private List<String> patronAgencyCodes;
  private List<String> itemAgencyCodes;
  private List<Integer> patronTypes;
  private List<Integer> centralItemTypes;
}
