package org.folio.innreach.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class JsonUtils {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static final String OBJECT_SERIALIZATION_FAILED = "Failed to serialize object to a json string";
  public static final String OBJECT_DESERIALIZATION_FAILED = "Failed to deserialize json string to an object";


  public static String toJson(Object o) {
    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      log.info(OBJECT_SERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_SERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return jsonString;
  }

  public static <T> T fromJson(String jsonString, Class<T> valueType) {
    T obj;
    try {
      obj = mapper.readValue(jsonString, valueType);
    } catch (JsonProcessingException e) {
      log.info(OBJECT_DESERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_DESERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return obj;
  }

}
