package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.util.JsonHelper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public abstract class OngoingContributionStatusMapper {

  private JsonHelper jsonHelper;

  @Autowired
  public void setJsonHelper(JsonHelper jsonHelper) {
    this.jsonHelper = jsonHelper;
  }

  public abstract List<OngoingContributionStatus> convertItemListToEntities(List<DomainEvent<org.folio.innreach.dto.Item>> domainEvent);

  @Mapping(target = "newEntity", expression = "java(setNewEntity(domainEvent))")
  @Mapping(target = "oldEntity", expression = "java(setOldEntity(domainEvent))")
  @Mapping(target = "domainEventName", constant = "ITEM")
  @Mapping(target = "domainEventType", source = "domainEvent.type")
  @Mapping(target = "status", constant = "READY")
  public abstract OngoingContributionStatus convertItemToEntity(DomainEvent<org.folio.innreach.dto.Item> domainEvent);

  public  <T> String setNewEntity(DomainEvent<T> domainEvent) {
    return jsonHelper.toJson(domainEvent.getData().getNewEntity());
  }

  public  <T> String setOldEntity(DomainEvent<T> domainEvent) {
    return jsonHelper.toJson(domainEvent.getData().getOldEntity());
  }
}
