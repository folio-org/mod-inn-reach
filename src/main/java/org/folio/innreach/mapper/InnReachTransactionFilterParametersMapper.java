package org.folio.innreach.mapper;

import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionFilterParametersMapper {
  @Mapping(target = "types", source = "dto.type")
  @Mapping(target = "states", source = "dto.state")
  @Mapping(target = "centralServerCodes", source = "dto.centralServerCode")
  @Mapping(target = "patronAgencyCodes", source = "dto.patronAgencyCode")
  @Mapping(target = "itemAgencyCodes", source = "dto.itemAgencyCode")
  @Mapping(target = "patronTypes", source = "dto.patronType")
  @Mapping(target = "centralItemTypes", source = "dto.centralItemType")
  InnReachTransactionFilterParameters toEntity(InnReachTransactionFilterParametersDTO dto);
}
