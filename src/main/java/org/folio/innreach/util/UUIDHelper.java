package org.folio.innreach.util;

import java.util.UUID;

public class UUIDHelper {

  public static String toStringWithoutHyphens(UUID uuid) {
    return uuid.toString().replace("-", "");
  }

  public static UUID fromStringWithoutHyphens(String uuid) {
    var strUuid = String.format("%s-%s-%s-%s-%s",
      uuid.substring(0, 8),
      uuid.substring(8, 12),
      uuid.substring(12, 16),
      uuid.substring(16, 20),
      uuid.substring(20));
    return UUID.fromString(strUuid);
  }
}
