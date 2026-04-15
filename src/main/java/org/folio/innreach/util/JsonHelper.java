package org.folio.innreach.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Component
public class JsonHelper {

  @Value("${inn-reach.checkout-time.duration}")
  private long defaultCheckoutTimeDuration;

  public static final String CHECKOUT_TIMEOUT_DURATION = "checkoutTimeoutDuration";
  private final ObjectMapper mapper;

  public static final String OBJECT_SERIALIZATION_FAILED = "Failed to serialize object to a json string";
  public static final String OBJECT_DESERIALIZATION_FAILED = "Failed to deserialize json string to an object";


  public String toJson(Object o) {
    if (o == null)
      return null;
    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(o);
    } catch (JacksonException e) {
      log.info(OBJECT_SERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_SERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return jsonString;
  }

  public <T> T fromJson(String jsonString, Class<T> valueType) {
    if (jsonString == null)
      return null;
    T obj;
    try {
      obj = mapper.readValue(jsonString, valueType);
    } catch (JacksonException e) {
      log.info(OBJECT_DESERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_DESERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return obj;
  }

  public <T> T fromJson(InputStream inputStream, Class<T> valueType) throws IOException {
    T obj;
    try {
      obj = mapper.readValue(inputStream, valueType);
    } catch (JacksonException e) {
      log.info(OBJECT_DESERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_DESERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return obj;
  }


  public Long getCheckoutTimeDurationInMilliseconds(List<CirculationSettingDTO> configData) {
    long duration = configData.stream()
      .findFirst()
      .map(CirculationSettingDTO::getValue)
      .map(v -> v.get(CHECKOUT_TIMEOUT_DURATION))
      .filter(Number.class::isInstance)
      .map(Number.class::cast)
      .map(Number::longValue)
      .orElse(defaultCheckoutTimeDuration);
    return duration * 60000;
  }

}
