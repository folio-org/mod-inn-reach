package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.TransactionStateEnum;
import org.folio.innreach.dto.TransactionTypeEnum;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.InnReachTransactionFilterParameters;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;

import java.util.Arrays;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionFilterParametersMapper {
  @Mapping(target = "types", source = "dto.type")
  @Mapping(target = "states", source = "dto.state")
  @Mapping(target = "centralServerCodes", source = "dto.centralServerCode")
  @Mapping(target = "patronAgencyCodes", source = "dto.patronAgencyCode")
  @Mapping(target = "itemAgencyCodes", source = "dto.itemAgencyCode")
  @Mapping(target = "patronTypes", source = "dto.centralPatronType")
  @Mapping(target = "centralItemTypes", source = "dto.centralItemType")
  InnReachTransactionFilterParameters toEntity(InnReachTransactionFilterParametersDTO dto);

  default InnReachTransactionFilterParameters.SortBy toEntityEnum(InnReachTransactionFilterParametersDTO.SortByEnum sort){
    return sort == null ? null : Arrays.stream(InnReachTransactionFilterParameters.SortBy.values())
      .filter(e -> e.getValue().equals(sort.getValue())).findFirst().orElseThrow(() ->
        new IllegalArgumentException("No enum found for value " + sort.getValue()));
  }

  default InnReachTransactionFilterParameters.SortOrder toEntityEnum(InnReachTransactionFilterParametersDTO.SortOrderEnum sort){
    return sort == null ? null : Arrays.stream(InnReachTransactionFilterParameters.SortOrder.values())
      .filter(e -> e.getValue().equals(sort.getValue())).findFirst().orElseThrow(() ->
        new IllegalArgumentException("No enum found for value " + sort.getValue()));
  }

  default InnReachTransaction.TransactionType toEntityEnum(TransactionTypeEnum type){
    return type == null ? null : Arrays.stream(InnReachTransaction.TransactionType.values())
      .filter(e -> e.getValue().equals(type.getValue())).findFirst().orElseThrow(() ->
        new IllegalArgumentException("No enum found for value " + type.getValue()));
  }

  default InnReachTransaction.TransactionState toEntityEnum(TransactionStateEnum state) {
    return state == null ? null : Arrays.stream(InnReachTransaction.TransactionState.values())
      .filter(e -> e.getValue().equals(state.getValue())).findFirst().orElseThrow(() ->
        new IllegalArgumentException("No enum found for value " + state.getValue()));
  }
}
