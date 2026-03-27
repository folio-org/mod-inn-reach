package org.folio.innreach.util;

import static org.folio.innreach.util.JsonHelper.CHECKOUT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.junit.jupiter.api.Test;

class JsonHelperTest {

  private static final long MINUTES_TO_MS = 60000L;

  @Test
  void getCheckoutTimeDurationInMilliseconds_returnsValueFromSetting() {
    var setting = new CirculationSettingDTO("id", "other_settings",
      Map.of(CHECKOUT_TIMEOUT_DURATION, 5));

    var result = JsonHelper.getCheckoutTimeDurationInMilliseconds(List.of(setting));

    assertEquals(5 * MINUTES_TO_MS, result);
  }

  @Test
  void getCheckoutTimeDurationInMilliseconds_emptyList_returnsDefault() {
    var result = JsonHelper.getCheckoutTimeDurationInMilliseconds(List.of());

    assertEquals(0L, result);
  }

  @Test
  void getCheckoutTimeDurationInMilliseconds_nullValueMap_returnsDefault() {
    var setting = new CirculationSettingDTO("id", "other_settings", null);

    var result = JsonHelper.getCheckoutTimeDurationInMilliseconds(List.of(setting));

    assertEquals(0L, result);
  }

  @Test
  void getCheckoutTimeDurationInMilliseconds_missingKey_returnsDefault() {
    var setting = new CirculationSettingDTO("id", "other_settings",
      Map.of("someOtherKey", 10));

    var result = JsonHelper.getCheckoutTimeDurationInMilliseconds(List.of(setting));

    assertEquals(0L, result);
  }

  @Test
  void getCheckoutTimeDurationInMilliseconds_nonNumberValue_returnsDefault() {
    var setting = new CirculationSettingDTO("id", "other_settings",
      Map.of(CHECKOUT_TIMEOUT_DURATION, "notANumber"));

    var result = JsonHelper.getCheckoutTimeDurationInMilliseconds(List.of(setting));

    assertEquals(0L, result);
  }
}
