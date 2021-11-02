package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InnReachTransactionSortingParameters {
  public List<InnReachTransaction.TransactionType> types;
  public List<InnReachTransaction.TransactionState> states;
  public List<String> centralServerCodes;
  public List<String> patronAgencyCodes;
  public List<String> itemAgencyCodes;
  public List<Integer> patronTypes;
  public List<Integer> centralItemTypes;
}
