package org.folio.innreach.mapper;

import org.mapstruct.Mapping;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "createdDate", source = "metadata.createdDate")
@Mapping(target = "createdBy.name", source = "metadata.createdByUsername")
@Mapping(target = "createdBy.id", source = "metadata.createdByUserId")
@Mapping(target = "updatedDate", source = "metadata.updatedDate")
@Mapping(target = "updatedBy.name", source = "metadata.updatedByUsername")
@Mapping(target = "updatedBy.id", source = "metadata.updatedByUserId")
public @interface AuditableMappingToEntity {
}
