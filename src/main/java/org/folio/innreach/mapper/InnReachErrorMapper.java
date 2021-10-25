package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.validation.FieldError;

import org.folio.innreach.dto.InnReachError;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface InnReachErrorMapper {

  @Mapping(target = "name", source = "error.field")
  @Mapping(target = "rejectedValue", source = "error.rejectedValue")
  @Mapping(target = "reason", source = "error.defaultMessage")
  InnReachError toInnReachError(FieldError error);

  @Mapping(target = "reason", source = "e.message")
  InnReachError toInnReachError(Exception e);

  default String map(Object value){
    return value.toString();
  }
}
