package org.folio.innreach.external.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.dto.BibInfoResponseDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.PatronInfoResponseDTO;
import org.folio.innreach.external.dto.BibInfoResponse;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.dto.PatronInfoResponse;
import org.folio.innreach.mapper.MappingMethods;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface InnReachResponseMapper {

  InnReachResponseDTO toDto(InnReachResponse response);

  BibInfoResponseDTO toDto(BibInfoResponse response);

  PatronInfoResponseDTO toDto(PatronInfoResponse response);

}
