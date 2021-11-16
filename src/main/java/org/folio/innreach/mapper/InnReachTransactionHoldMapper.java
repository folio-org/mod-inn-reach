package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {MappingMethods.class, InnReachTransactionPickupLocationMapper.class})
public interface InnReachTransactionHoldMapper {

  @AuditableMapping
  @Mapping(target = "centralPatronType", source = "transactionItemHold.centralPatronTypeItem")
  TransactionHoldDTO toItemHoldDTO(TransactionItemHold transactionItemHold);

  @Mapping(target = "centralPatronTypeItem", source = "dto.centralPatronType")
  TransactionItemHold toItemHold(TransactionHoldDTO dto);

  @AuditableMapping
  @Mapping(target = "centralPatronType", source = "transactionLocalHold.centralPatronTypeLocal")
  TransactionHoldDTO toLocalHoldDTO(TransactionLocalHold transactionLocalHold);

  @Mapping(target = "centralPatronTypeLocal", source = "dto.centralPatronType")
  TransactionLocalHold toLocalHold(TransactionHoldDTO dto);

  @AuditableMapping
  TransactionHoldDTO toPatronHoldDTO(TransactionPatronHold transactionPatronHold);

  TransactionPatronHold toPatronHold(TransactionHoldDTO dto);

  TransactionHoldDTO mapRequest(PatronHoldDTO dto);
}
