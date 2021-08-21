package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.dto.InnReachLocationDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface InnReachLocationMapper {

	InnReachLocation mapToInnReachLocation(InnReachLocationDTO innReachLocationDTO);

  @AuditableMapping
	InnReachLocationDTO mapToInnReachLocationDTO(InnReachLocation innReachLocation);
}
