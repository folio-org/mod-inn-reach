package org.folio.innreach.mapper;

import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.folio.innreach.dto.InnReachRecallUserDTO;

@Mapper(componentModel = "spring")
public interface InnReachRecallUserMapper {

  InnReachRecallUserDTO toDto(InnReachRecallUser recallUser);

  InnReachRecallUser toEntity(InnReachRecallUserDTO recallUser);

}
