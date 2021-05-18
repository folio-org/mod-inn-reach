package org.folio.innreach.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.UUID;

public class TestUtil {

  public static String randomUUIDString() {
    return UUID.randomUUID().toString();
  }

  public static String randomFiveCharacterCode() {
    return randomUUIDString().substring(0, 5);
  }

  @SneakyThrows
  public static <T> T deserializeFromJsonFile(String path, Class<T> type) {
    var objectMapper = new ObjectMapper();
    return objectMapper.readValue(TestUtil.class.getResource("/json" + path), type);
  }
}
