package org.folio.innreach.mapper;

import com.google.common.collect.Lists;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.entity.TransactionPickupLocation;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionHold;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionMapper {

  String PICKUP_LOCATION_DELIMITER = ":";

  TransactionItemHold toItemHold(TransactionItemHoldDTO dto);

  default TransactionPickupLocation map(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Pickup location must not be null.");
    }
    var strings = value.split(PICKUP_LOCATION_DELIMITER);
    if (strings.length > 4 || strings.length < 3) {
      throw new IllegalArgumentException("Pickup location must consist of 3 or 4 strings delimited by a colon.");
    }
    var pickupLocation = new TransactionPickupLocation();
    pickupLocation.setPickupLocCode(strings[0]);
    pickupLocation.setDisplayName(strings[1]);
    pickupLocation.setPrintName(strings[2]);
    if (strings.length > 3) {
      pickupLocation.setDeliveryStop(strings[3]);
    }
    return pickupLocation;
  }

  default String map(TransactionPickupLocation value) {
    var locationTokens = Lists.newArrayList(value.getPickupLocCode(), value.getDisplayName(), value.getPrintName());

    if (value.getDeliveryStop() != null) {
      locationTokens.add(value.getDeliveryStop());
    }

    return String.join(PICKUP_LOCATION_DELIMITER, locationTokens);
  }

  @Mapping(target = "hold", ignore = true)
  @AuditableMapping
  InnReachTransactionDTO toDTO(InnReachTransaction entity);

  InnReachTransactionHold toHoldDTO(TransactionItemHold itemHold);
  InnReachTransactionHold toHoldDTO(TransactionPatronHold patronHold);
  InnReachTransactionHold toHoldDTO(TransactionLocalHold localHold);

  default List<InnReachTransactionDTO> toDTOs(Iterable<InnReachTransaction> entities){
    List<InnReachTransactionDTO> dtos = new LinkedList<>();
    for (InnReachTransaction transaction : entities) {
      var dto = toDTO(transaction);
      switch (transaction.getType()){
        case ITEM:
          dto.setHold(toHoldDTO((TransactionItemHold) transaction.getHold()));
          break;
        case LOCAL:
          dto.setHold(toHoldDTO((TransactionLocalHold) transaction.getHold()));
          break;
        case PATRON:
          dto.setHold(toHoldDTO((TransactionPatronHold) transaction.getHold()));
          break;
        default:
          break;
      }
      dtos.add(dto);
    }
    return dtos;
  }

  default InnReachTransactionsDTO toDTOCollection(Page<InnReachTransaction> pageable) {
    List<InnReachTransactionDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new InnReachTransactionsDTO().transactions(dtos).totalRecords((int) pageable.getTotalElements());
  }
}
