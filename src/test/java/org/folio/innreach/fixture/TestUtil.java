package org.folio.innreach.fixture;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.domain.entity.CentralServer;

public class TestUtil {

  public static String randomUUIDString() {
    return UUID.randomUUID().toString();
  }

  public static String randomFiveCharacterCode() {
    return randomUUIDString().substring(0, 5);
  }

  public static int randomInteger(int range) {
    return new Random().nextInt(range);
  }

  public static int randomIntegerExcept(int range, Set<Integer> except){
    Random random = new Random();
    int integer;
    do {
      integer = random.nextInt(range);
    } while (except.contains(integer));
    return integer;
  }

  @SneakyThrows
  public static <T> T deserializeFromJsonFile(String path, Class<T> type) {
    var objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper.readValue(TestUtil.class.getResource("/json" + path), type);
  }

  public static CentralServer refCentralServer(UUID id) {
    var centralServer = new CentralServer();
    centralServer.setId(id);
    return centralServer;
  }

  public static void setEnvProperty(String value) {
    setProperty("env", value);
  }

  public static void removeEnvProperty() {
    clearProperty("env");
  }

  public static RetryTemplate createNoRetryTemplate() {
    return RetryTemplate.builder()
      .noBackoff()
      .notRetryOn(Exception.class)
      .build();
  }
}
