package org.folio.innreach.fixture;

import java.util.UUID;

public class FixtureUtil {

  public static String randomUUIDString() {
    return UUID.randomUUID().toString();
  }

  public static String randomFiveCharacterCode() {
    return randomUUIDString().substring(0, 5);
  }
}
