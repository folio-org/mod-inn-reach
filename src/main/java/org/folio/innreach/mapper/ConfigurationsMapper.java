package org.folio.innreach.mapper;

import org.apache.commons.lang3.StringUtils;
import org.folio.innreach.dto.ModuleConfiguration;
import org.folio.innreach.dto.ModuleConfigurations;
import org.folio.innreach.domain.entity.Configuration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ConfigurationsMapper {

  @Mappings({
    @Mapping(target = "id", expression = "java(uuidToStringSafe(configuration.getId()))"),
    @Mapping(target = "name", source = "name"),
    @Mapping(target = "providerName", source = "providerName"),
    @Mapping(target = "url", source = "url"),
    @Mapping(target = "accessionDelay", source = "accessionDelay"),
    @Mapping(target = "accessionTimeUnit", expression = "java(org.folio.innreach.dto.TimeUnits.fromValue(configuration.getAccessionTimeUnit()))"),
    @Mapping(target = "metadata.createdDate", source = "createdDate"),
    @Mapping(target = "metadata.updatedDate", source = "updatedDate"),
    @Mapping(target = "metadata.createdByUserId", expression = "java(configuration.getCreatedByUserId() == null ? null : String.valueOf(configuration.getCreatedByUserId()))"),
    @Mapping(target = "metadata.updatedByUserId", expression = "java(configuration.getUpdatedByUserId() == null ? null : String.valueOf(configuration.getUpdatedByUserId()))"),
    @Mapping(target = "metadata.createdByUsername", source = "createdByUsername"),
    @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  })
  ModuleConfiguration mapEntityToDto(Configuration configuration);

  @Mappings({
    @Mapping(target = "id", expression = "java(stringToUUIDSafe(moduleConfiguration.getId()))"),
    @Mapping(target = "accessionTimeUnit", expression = "java(moduleConfiguration.getAccessionTimeUnit() == null ? null : moduleConfiguration.getAccessionTimeUnit().toString())"),
    @Mapping(target = "createdByUserId", expression = "java(moduleConfiguration.getMetadata() == null ? null : stringToUUIDSafe(moduleConfiguration.getMetadata().getCreatedByUserId()))"),
    @Mapping(target = "updatedByUserId", expression = "java(moduleConfiguration.getMetadata() == null ? null : stringToUUIDSafe(moduleConfiguration.getMetadata().getUpdatedByUserId()))")
  })
  @InheritInverseConfiguration
  Configuration mapDtoToEntity(ModuleConfiguration moduleConfiguration);

  @Mappings({})
  List<ModuleConfiguration> mapEntitiesToDtos(Iterable<Configuration> remoteModuleConfigurationList);

  default ModuleConfigurations mapEntitiesToRemoteConfigCollection(Iterable<Configuration> remoteModuleConfigurationList) {
    List<ModuleConfiguration> remoteConfigList = mapEntitiesToDtos(remoteModuleConfigurationList);
    return new ModuleConfigurations().configurations(remoteConfigList).totalRecords(remoteConfigList.size());
  }

  default UUID stringToUUIDSafe(String uuid) {
    return (StringUtils.isBlank(uuid)) ? null : UUID.fromString(uuid);
  }

  default String uuidToStringSafe(UUID uuid) {
    return uuid != null ? uuid.toString() : null;
  }
}
