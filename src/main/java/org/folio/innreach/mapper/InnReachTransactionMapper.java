package org.folio.innreach.mapper;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.Metadata;
import org.folio.innreach.dto.TransactionHoldDTO;

@Component
@RequiredArgsConstructor
public class InnReachTransactionMapper {

  private final InnReachTransactionHoldMapper innReachTransactionHoldMapper;
  private final MappingMethods mappingMethods;

  private Map<Class<? extends TransactionHold>, Function<TransactionHold, TransactionHoldDTO>> holdMappers;

  @PostConstruct
  public void initTransactionHoldMappers() {
    this.holdMappers = Map.of(
      TransactionItemHold.class, hold -> innReachTransactionHoldMapper.toItemHoldDTO((TransactionItemHold) hold),
      TransactionLocalHold.class, hold -> innReachTransactionHoldMapper.toLocalHoldDTO((TransactionLocalHold) hold),
      TransactionPatronHold.class, hold -> innReachTransactionHoldMapper.toPatronHoldDTO((TransactionPatronHold) hold)
    );
  }

  public InnReachTransactionDTO toDto(InnReachTransaction innReachTransaction) {
    var innReachTransactionDTO = new InnReachTransactionDTO()
      .id(innReachTransaction.getId())
      .trackingId(innReachTransaction.getTrackingId())
      .centralServerCode(innReachTransaction.getCentralServerCode());

    if (innReachTransaction.getState() != null) {
      innReachTransactionDTO.state(innReachTransaction.getState().toString());
    }

    if (innReachTransaction.getType() != null) {
      innReachTransactionDTO.type(innReachTransaction.getType().toString());
    }

    if (innReachTransaction.getHold() != null) {
      innReachTransactionDTO.transactionHold(holdMappers.get(innReachTransaction.getHold().getClass()).apply(innReachTransaction.getHold()));
    }
      
    innReachTransactionDTO.setMetadata(collectMetadata(innReachTransaction));
    return innReachTransactionDTO;
  }

  private Metadata collectMetadata(InnReachTransaction innReachTransaction) {
    var metadata = new Metadata()
      .createdDate(mappingMethods.offsetDateTimeAsDate(innReachTransaction.getCreatedDate()))
      .updatedDate(mappingMethods.offsetDateTimeAsDate(innReachTransaction.getUpdatedDate()));

    if (innReachTransaction.getCreatedBy() != null) {
      metadata.createdByUserId(mappingMethods.uuidAsString(innReachTransaction.getCreatedBy().getId()))
        .createdByUsername(innReachTransaction.getCreatedBy().getName());
    }

    if (innReachTransaction.getUpdatedBy() != null) {
      metadata.updatedByUserId(mappingMethods.uuidAsString(innReachTransaction.getUpdatedBy().getId()))
        .updatedByUsername(innReachTransaction.getUpdatedBy().getName());
    }

    return metadata;
  }
}
