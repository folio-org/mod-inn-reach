package org.folio.innreach.mapper;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.dto.InnReachLocationDTO;

@Mapper(componentModel = "spring")
public interface InnReachLocationMapper {

	@Mapping(target = "id", expression = "java(stringToUUID(innReachLocationDTO.getId()))")
	InnReachLocation mapToInnReachLocation(InnReachLocationDTO innReachLocationDTO);

	@Mapping(target = "id", expression = "java(uuidToString(innReachLocation.getId()))")
  @Mapping(target = "metadata.createdDate", expression = "java(offsetDateTimeToDate(innReachLocation.getCreatedDate()))")
  @Mapping(target = "metadata.createdByUserId", source = "innReachLocation.createdBy")
  @Mapping(target = "metadata.updatedDate", expression = "java(offsetDateTimeToDate(innReachLocation.getLastModifiedDate()))")
  @Mapping(target = "metadata.updatedByUserId", source = "innReachLocation.lastModifiedBy")
	InnReachLocationDTO mapToInnReachLocationDTO(InnReachLocation innReachLocation);

  default String uuidToString(UUID uuid) {
    return uuid == null ? null : uuid.toString();
	}

	default UUID stringToUUID(String uuid) {
    return uuid == null ? null : UUID.fromString(uuid);
  }

  default Date offsetDateTimeToDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }
}
