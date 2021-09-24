package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPickupLocation;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionMapper {

  TransactionItemHold toItemHold(TransactionItemHoldDTO dto);

  default TransactionPickupLocation map(String value) {
    var strings = value.split(":");
    var pickupLocation = new TransactionPickupLocation();
    pickupLocation.setPickupLocCode(strings[0]);
    pickupLocation.setDisplayName(strings[1]);
    pickupLocation.setPrintName(strings[2]);
    if (strings.length > 3) {
      pickupLocation.setDeliveryStop(strings[3]);
    }
    return pickupLocation;
  }
}
