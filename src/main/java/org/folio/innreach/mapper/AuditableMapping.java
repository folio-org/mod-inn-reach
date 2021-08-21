package org.folio.innreach.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.mapstruct.Mapping;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "metadata.createdDate", source = "createdDate")
@Mapping(target = "metadata.createdByUsername", source = "createdBy.name")
@Mapping(target = "metadata.createdByUserId", source = "createdBy.id")
@Mapping(target = "metadata.updatedDate", source = "updatedDate")
@Mapping(target = "metadata.updatedByUsername", source = "updatedBy.name")
@Mapping(target = "metadata.updatedByUserId", source = "updatedBy.id")
public @interface AuditableMapping {
}
