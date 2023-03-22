package org.folio.innreach.mapper;

import com.google.common.collect.Lists;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.TransactionPickupLocation;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface InnReachTransactionPickupLocationMapper {

  String PICKUP_LOCATION_DELIMITER = ":";
  int PICKUP_LOCATION_CODE_POSITION = 0;
  int DISPLAY_NAME_POSITION = 1;
  int DELIVERY_STOP_POSITION = 2;

  default TransactionPickupLocation fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Pickup location must not be null.");
    }

    var strings = value.split(PICKUP_LOCATION_DELIMITER);

    if (strings.length != 3) {
      throw new IllegalArgumentException("Pickup location must consist of 3 strings delimited by a colon.");
    }

    var pickupLocation = new TransactionPickupLocation();
    pickupLocation.setPickupLocCode(strings[PICKUP_LOCATION_CODE_POSITION]);
    pickupLocation.setDisplayName(strings[DISPLAY_NAME_POSITION]);
    pickupLocation.setDeliveryStop(strings[DELIVERY_STOP_POSITION]);

//    if (strings.length > 3) {
//      pickupLocation.setDeliveryStop(strings[DELIVERY_STOP_POSITION]);
//    }

    return pickupLocation;
  }

  default String toString(TransactionPickupLocation value) {
    var locationTokens = Lists.newArrayList(value.getPickupLocCode(), value.getDisplayName(), value.getDeliveryStop());

//    if (value.getDeliveryStop() != null) {
//      locationTokens.add(value.getDeliveryStop());
//    }

    return String.join(PICKUP_LOCATION_DELIMITER, locationTokens);
  }
}
