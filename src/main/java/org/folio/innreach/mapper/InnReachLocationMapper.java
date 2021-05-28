package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.dto.InnReachLocationDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface InnReachLocationMapper {

	InnReachLocation mapToInnReachLocation(InnReachLocationDTO innReachLocationDTO);

  @Mapping(target = "metadata.createdDate", source = "innReachLocation.createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "innReachLocation.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "innReachLocation.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "innReachLocation.lastModifiedBy")
	InnReachLocationDTO mapToInnReachLocationDTO(InnReachLocation innReachLocation);
}
