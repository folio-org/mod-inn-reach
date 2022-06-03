package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.PagingSlipTemplate;
import org.folio.innreach.dto.PagingSlipTemplateDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface PagingSlipTemplateMapper {

  PagingSlipTemplate toEntity(PagingSlipTemplateDTO dto);

  @AuditableMapping
  PagingSlipTemplateDTO toDTO(PagingSlipTemplate entity);

}
