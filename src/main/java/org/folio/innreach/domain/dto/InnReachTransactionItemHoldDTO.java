package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.dto.Metadata;
import org.folio.innreach.dto.TransactionItemHoldDTO;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InnReachTransactionItemHoldDTO {
  private UUID id;
  private String trackingId;
  private String centralServerCode;
  private TransactionState state;
  private TransactionType type;
  private TransactionItemHoldDTO hold;
  private Metadata metadata;
}
