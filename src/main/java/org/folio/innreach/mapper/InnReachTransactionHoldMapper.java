package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.TransactionHoldDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {MappingMethods.class, InnReachTransactionPickupLocationMapper.class})
public interface InnReachTransactionHoldMapper {

  TransactionHoldDTO toItemHoldDTO(TransactionItemHold transactionItemHold);

  TransactionItemHold toItemHold(TransactionHoldDTO dto);

  TransactionHoldDTO toLocalHoldDTO(TransactionLocalHold transactionLocalHold);

  TransactionLocalHold toLocalHold(TransactionHoldDTO dto);

  TransactionHoldDTO toPatronHoldDTO(TransactionPatronHold transactionPatronHold);

  TransactionPatronHold toPatronHold(TransactionHoldDTO dto);
}
