package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.InnReachTransactionItemHoldDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionMapper {

  TransactionItemHold toItemHold(TransactionItemHoldDTO dto);

  @AuditableMapping
  InnReachTransactionItemHoldDTO toInnReachTransactionItemHoldDTO(InnReachTransaction entity);
}
