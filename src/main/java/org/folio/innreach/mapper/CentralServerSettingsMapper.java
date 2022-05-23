package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.CentralServerSettings;
import org.folio.innreach.dto.CentralServerSettingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface CentralServerSettingsMapper {

  CentralServerSettings mapToCentralServerSettings(CentralServerSettingsDTO centralServerSettingsDTO);
  CentralServerSettingsDTO mapToCentralServiceSettingsDTO(CentralServerSettings centralServerSettings);
}
