package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.TransactionHoldDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {MappingMethods.class, InnReachTransactionMapper.class})
public interface TransactionHoldMapper {

  TransactionHoldDTO toItemHoldDTO(TransactionItemHold transactionItemHold);

  TransactionHoldDTO toLocalHoldDTO(TransactionLocalHold transactionLocalHold);

  TransactionHoldDTO toPatronHoldDTO(TransactionPatronHold transactionPatronHold);
}
