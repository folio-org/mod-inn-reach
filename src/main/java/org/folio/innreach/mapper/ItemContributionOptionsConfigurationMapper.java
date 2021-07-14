package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemContributionOptionsConfigurationMapper {
  ItemContributionOptionsConfiguration toEntity(ItemContributionOptionsConfigurationDTO itemContributionOptionsConfigurationDTO);

  ItemContributionOptionsConfigurationDTO toDto(ItemContributionOptionsConfiguration createdItemContributionOptionsConfiguration);
}
