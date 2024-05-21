package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.util.JsonHelper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public abstract class OngoingContributionStatusMapper {

  @Autowired
  private JsonHelper jsonHelper;

  @Mapping(target = "newEntity", expression = "java(setNewEntity(domainEvent))")
  @Mapping(target = "oldEntity", expression = "java(setOldEntity(domainEvent))")
  @Mapping(target = "domainEventType", constant = "ITEM")
  @Mapping(target = "actionType", source = "domainEvent.type")
  public abstract OngoingContributionStatus toEntity(DomainEvent<org.folio.innreach.dto.Item> domainEvent);

  public  <T> String setNewEntity(DomainEvent<T> domainEvent) {
    return jsonHelper.toJson(domainEvent.getData().getNewEntity());
  }

  public  <T> String setOldEntity(DomainEvent<T> domainEvent) {
    return jsonHelper.toJson(domainEvent.getData().getOldEntity());
  }
}
