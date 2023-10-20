package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface JobExecutionStatusMapper {

  @Mapping(target = "status", constant = "READY")
  JobExecutionStatus toEntity(InstanceIterationEvent instanceIterationEvent);

}
