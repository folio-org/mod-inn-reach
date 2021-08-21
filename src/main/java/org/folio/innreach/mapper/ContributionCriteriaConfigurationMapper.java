package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.dto.ContributionCriteriaDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface ContributionCriteriaConfigurationMapper {

  @Mapping(target = "contributeButSuppressCodeId", source = "contributeButSuppressId")
  @Mapping(target = "contributeAsSystemOwnedCodeId", source = "contributeAsSystemOwnedId")
  @Mapping(target = "doNotContributeCodeId", source = "doNotContributeId")
  @Mapping(target = "excludedLocationIds", source = "locationIds")
  ContributionCriteriaConfiguration toEntity(ContributionCriteriaDTO dto);

  @Mapping(target = "contributeButSuppressId", source = "contributeButSuppressCodeId")
  @Mapping(target = "contributeAsSystemOwnedId", source = "contributeAsSystemOwnedCodeId")
  @Mapping(target = "doNotContributeId", source = "doNotContributeCodeId")
  @Mapping(target = "locationIds", source = "excludedLocationIds")
  @AuditableMapping
  ContributionCriteriaDTO toDTO(ContributionCriteriaConfiguration entity);

}
