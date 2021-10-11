package org.folio.innreach.mapper;

import com.google.common.collect.Lists;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPickupLocation;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

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
}
