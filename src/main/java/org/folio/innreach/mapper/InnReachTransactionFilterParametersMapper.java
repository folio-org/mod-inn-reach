package org.folio.innreach.mapper;

import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionFilterParametersMapper {
  InnReachTransactionFilterParameters toEntity(InnReachTransactionFilterParametersDTO dto);
}
