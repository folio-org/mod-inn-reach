package org.folio.innreach.mapper;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.ContributionError;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.ContributionsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface ContributionMapper {

  Contribution toEntity(ContributionDTO dto);

  ContributionError toEntity(ContributionErrorDTO dto);

  @Mapping(target = "contributionStarted", source = "entity.createdDate")
  @Mapping(target = "contributionStartedBy", source = "entity.createdBy.name")
  @Mapping(target = "contributionComplete", source = "entity.completeDate")
  @AuditableMapping
  ContributionDTO toDTO(Contribution entity);

  List<Contribution> toEntities(Iterable<ContributionDTO> dtos);

  List<ContributionDTO> toDTOs(Iterable<Contribution> entities);

  @ValueMapping(source = MappingConstants.NULL, target = "NOT_STARTED")
  ContributionDTO.StatusEnum toDTO(Contribution.Status entity);

  default ContributionsDTO toDTOCollection(Page<Contribution> pageable) {
    List<ContributionDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new ContributionsDTO()
      .contributionHistory(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }

}
