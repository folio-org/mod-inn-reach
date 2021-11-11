package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.TransactionHoldDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CirculationRequestMapper {

  @Mapping(source = "circulationRequest.itemBarcode", target = "shippedItemBarcode")
  TransactionHoldDTO toTransactionHoldDTO(CirculationRequestDTO circulationRequest);
}
