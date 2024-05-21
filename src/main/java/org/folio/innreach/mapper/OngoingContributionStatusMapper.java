package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.event.DomainEvent;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface OngoingContributionStatusMapper {

  @Mapping(target = "newEntity", source = "domainEvent.newEntity")
  @Mapping(target = "oldEntity", source = "domainEvent.oldEntity")
  @Mapping(target = "domainEventType", expression = "java(setDomainEventType(domainEvent))")
  @Mapping(target = "actionType", source = "domainEvent.type")
  <T> OngoingContributionStatus toEntity(DomainEvent<T> domainEvent);

  default <T> String setDomainEventType(DomainEvent<T> domainEvent) {
    return domainEvent.getClass().getSimpleName().toLowerCase();
  }

}
