package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public abstract class InnReachTransactionMapper {
  @Autowired
  InnReachTransactionHoldMapper holdMapper;

  @Mapping(target = "hold", ignore = true)
  @AuditableMapping
  public abstract InnReachTransactionDTO toDTOWithoutHold(InnReachTransaction entity);

  public InnReachTransactionDTO toDTO(InnReachTransaction entity) {
    var dto = toDTOWithoutHold(entity);
    switch (entity.getType()) {
      case ITEM:
        dto.setHold(holdMapper.toItemHoldDTO((TransactionItemHold) entity.getHold()));
        break;
      case LOCAL:
        dto.setHold(holdMapper.toLocalHoldDTO((TransactionLocalHold) entity.getHold()));
        break;
      case PATRON:
        dto.setHold(holdMapper.toPatronHoldDTO((TransactionPatronHold) entity.getHold()));
        break;
      default:
        break;
    }
    return dto;
  }

  public List<InnReachTransactionDTO> toDTOs(Iterable<InnReachTransaction> entities) {
    List<InnReachTransactionDTO> dtos = new LinkedList<>();
    for (InnReachTransaction transaction : entities) {
      var dto = toDTO(transaction);
      dtos.add(dto);
    }
    return dtos;
  }

  public InnReachTransactionsDTO toDTOCollection(Page<InnReachTransaction> pageable) {
    List<InnReachTransactionDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new InnReachTransactionsDTO().transactions(dtos).totalRecords((int) pageable.getTotalElements());
  }
}
