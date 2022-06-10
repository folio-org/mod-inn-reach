package org.folio.innreach.util;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CqlHelper {

  public static String matchAny(Collection<UUID> values) {
    return values.stream()
      .filter(Objects::nonNull)
      .map(UUID::toString)
      .collect(Collectors.joining(" or "));
  }

}
