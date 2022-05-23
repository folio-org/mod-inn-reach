package org.folio.innreach.util;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class JsonHelper {

  private final ObjectMapper mapper;

  public static final String OBJECT_SERIALIZATION_FAILED = "Failed to serialize object to a json string";
  public static final String OBJECT_DESERIALIZATION_FAILED = "Failed to deserialize json string to an object";


  public String toJson(Object o) {
    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      log.info(OBJECT_SERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_SERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return jsonString;
  }

  public <T> T fromJson(String jsonString, Class<T> valueType) {
    T obj;
    try {
      obj = mapper.readValue(jsonString, valueType);
    } catch (JsonProcessingException e) {
      log.info(OBJECT_DESERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_DESERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return obj;
  }

  public <T> T fromJson(InputStream inputStream, Class<T> valueType) throws IOException {
    T obj;
    try {
      obj = mapper.readValue(inputStream, valueType);
    } catch (JsonProcessingException e) {
      log.info(OBJECT_DESERIALIZATION_FAILED, e);
      throw new IllegalStateException(OBJECT_DESERIALIZATION_FAILED + ": " + e.getMessage());
    }
    return obj;
  }

}
