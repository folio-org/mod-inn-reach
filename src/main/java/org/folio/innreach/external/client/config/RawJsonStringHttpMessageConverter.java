package org.folio.innreach.external.client.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.SmartHttpMessageConverter;

/**
 * A {@link SmartHttpMessageConverter} that reads {@code application/json} responses as raw
 * {@link String} without any Jackson deserialization/re-serialization. This ensures the JSON
 * body is returned exactly as received from the server.
 *
 * <p>This converter only supports {@link String} as the target type and is intended to be
 * registered before Jackson converters so that {@code @HttpExchange} methods returning
 * {@code String} receive the unmodified response body.</p>
 */
public class RawJsonStringHttpMessageConverter implements SmartHttpMessageConverter<String> {

  private static final List<MediaType> SUPPORTED_MEDIA_TYPES =
    List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL);

  @Override
  public boolean canRead(ResolvableType type, MediaType mediaType) {
    return String.class == type.toClass() && isSupportedMediaType(mediaType);
  }

  @Override
  public boolean canWrite(ResolvableType targetType, Class<?> valueClass, MediaType mediaType) {
    return String.class == valueClass && isSupportedMediaType(mediaType);
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return SUPPORTED_MEDIA_TYPES;
  }

  @Override
  public String read(ResolvableType type, HttpInputMessage inputMessage,
                     Map<String, Object> hints) throws IOException, HttpMessageNotReadableException {
    Charset charset = getCharset(inputMessage);
    return new String(inputMessage.getBody().readAllBytes(), charset);
  }

  @Override
  public void write(String value, ResolvableType type, MediaType contentType,
                    HttpOutputMessage outputMessage, Map<String, Object> hints)
    throws IOException, HttpMessageNotWritableException {
    Charset charset = (contentType != null && contentType.getCharset() != null)
      ? contentType.getCharset() : StandardCharsets.UTF_8;
    outputMessage.getBody().write(value.getBytes(charset));
  }

  private boolean isSupportedMediaType(MediaType mediaType) {
    if (mediaType == null) {
      return true;
    }
    for (MediaType supported : SUPPORTED_MEDIA_TYPES) {
      if (supported.includes(mediaType)) {
        return true;
      }
    }
    return false;
  }

  private Charset getCharset(HttpInputMessage inputMessage) {
    MediaType contentType = inputMessage.getHeaders().getContentType();
    if (contentType != null && contentType.getCharset() != null) {
      return contentType.getCharset();
    }
    return StandardCharsets.UTF_8;
  }
}

