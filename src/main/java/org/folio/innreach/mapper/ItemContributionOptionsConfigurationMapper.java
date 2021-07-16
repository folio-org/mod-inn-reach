package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface ItemContributionOptionsConfigurationMapper {
  ItemContributionOptionsConfiguration toEntity(ItemContributionOptionsConfigurationDTO itmContribOptConfDTO);

  @Mapping(target = "metadata.createdDate", source = "itmContribOptConf.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "itmContribOptConf.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "itmContribOptConf.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "itmContribOptConf.lastModifiedBy")
  ItemContributionOptionsConfigurationDTO toDto(ItemContributionOptionsConfiguration itmContribOptConf);
}
