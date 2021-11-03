package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.InnReachTransactionSortingParameters;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionFilterParametersMapper {
  InnReachTransactionSortingParameters toEntity(InnReachTransactionFilterParametersDTO dto);
}
