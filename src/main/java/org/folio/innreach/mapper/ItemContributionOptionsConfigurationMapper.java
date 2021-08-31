package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface ItemContributionOptionsConfigurationMapper {
  ItemContributionOptionsConfiguration toEntity(ItemContributionOptionsConfigurationDTO itmContribOptConfDTO);

  @AuditableMapping
  ItemContributionOptionsConfigurationDTO toDto(ItemContributionOptionsConfiguration itmContribOptConf);
}
