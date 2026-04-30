package org.folio.innreach.external.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class RawJsonStringHttpMessageConverterTest {

  private final RawJsonStringHttpMessageConverter converter = new RawJsonStringHttpMessageConverter();

  @Mock
  private HttpInputMessage inputMessage;

  @Mock
  private HttpOutputMessage outputMessage;

  @Test
  void canRead_withStringTypeAndJsonMediaType_returnsTrue() {
    assertTrue(converter.canRead(ResolvableType.forClass(String.class), MediaType.APPLICATION_JSON));
  }

  @Test
  void canRead_withStringTypeAndTextPlainMediaType_returnsTrue() {
    assertTrue(converter.canRead(ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN));
  }

  @Test
  void canRead_withStringTypeAndNullMediaType_returnsTrue() {
    assertTrue(converter.canRead(ResolvableType.forClass(String.class), null));
  }

  @Test
  void canRead_withNonStringType_returnsFalse() {
    assertFalse(converter.canRead(ResolvableType.forClass(Integer.class), MediaType.APPLICATION_JSON));
  }

  @Test
  void canRead_withStringTypeAndXmlMediaType_returnsTrueBecauseAllIsSupported() {
    assertTrue(converter.canRead(ResolvableType.forClass(String.class), MediaType.APPLICATION_XML));
  }

  @Test
  void canWrite_withStringClassAndJsonMediaType_returnsTrue() {
    assertTrue(converter.canWrite(ResolvableType.forClass(String.class), String.class, MediaType.APPLICATION_JSON));
  }

  @Test
  void canWrite_withNonStringClass_returnsFalse() {
    assertFalse(converter.canWrite(ResolvableType.forClass(Integer.class), Integer.class, MediaType.APPLICATION_JSON));
  }

  @Test
  void canWrite_withNullMediaType_returnsTrue() {
    assertTrue(converter.canWrite(ResolvableType.forClass(String.class), String.class, null));
  }

  @Test
  void getSupportedMediaTypes_returnsJsonTextPlainAndAll() {
    var types = converter.getSupportedMediaTypes();
    assertEquals(3, types.size());
    assertTrue(types.contains(MediaType.APPLICATION_JSON));
    assertTrue(types.contains(MediaType.TEXT_PLAIN));
    assertTrue(types.contains(MediaType.ALL));
  }

  @Test
  void read_withUtf8Content_returnsExactString() throws IOException {
    String json = "{\"key\":\"value\"}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    when(inputMessage.getHeaders()).thenReturn(headers);
    when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    String result = converter.read(ResolvableType.forClass(String.class), inputMessage, Map.of());

    assertEquals(json, result);
  }

  @Test
  void read_withExplicitCharsetInContentType_usesSpecifiedCharset() throws IOException {
    String json = "{\"key\":\"ñ\"}";
    MediaType mediaType = new MediaType("application", "json", StandardCharsets.ISO_8859_1);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    when(inputMessage.getHeaders()).thenReturn(headers);
    when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.ISO_8859_1)));

    String result = converter.read(ResolvableType.forClass(String.class), inputMessage, Map.of());

    assertEquals(json, result);
  }

  @Test
  void read_withNoContentType_defaultsToUtf8() throws IOException {
    String json = "{\"key\":\"value\"}";
    HttpHeaders headers = new HttpHeaders();
    when(inputMessage.getHeaders()).thenReturn(headers);
    when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    String result = converter.read(ResolvableType.forClass(String.class), inputMessage, Map.of());

    assertEquals(json, result);
  }

  @Test
  void read_withEmptyBody_returnsEmptyString() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    when(inputMessage.getHeaders()).thenReturn(headers);
    when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = converter.read(ResolvableType.forClass(String.class), inputMessage, Map.of());

    assertEquals("", result);
  }

  @Test
  void write_withUtf8ContentType_writesCorrectBytes() throws IOException {
    String value = "{\"key\":\"value\"}";
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(outputMessage.getBody()).thenReturn(outputStream);

    converter.write(value, ResolvableType.forClass(String.class), MediaType.APPLICATION_JSON, outputMessage, Map.of());

    assertEquals(value, outputStream.toString(StandardCharsets.UTF_8));
  }

  @Test
  void write_withNullContentType_defaultsToUtf8() throws IOException {
    String value = "{\"key\":\"value\"}";
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(outputMessage.getBody()).thenReturn(outputStream);

    converter.write(value, ResolvableType.forClass(String.class), null, outputMessage, Map.of());

    assertEquals(value, outputStream.toString(StandardCharsets.UTF_8));
  }

  @Test
  void write_withExplicitCharset_usesSpecifiedCharset() throws IOException {
    String value = "{\"key\":\"ñ\"}";
    MediaType mediaType = new MediaType("application", "json", StandardCharsets.ISO_8859_1);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(outputMessage.getBody()).thenReturn(outputStream);

    converter.write(value, ResolvableType.forClass(String.class), mediaType, outputMessage, Map.of());

    assertEquals(value, outputStream.toString(StandardCharsets.ISO_8859_1));
  }

  @Test
  void read_preservesJsonWhitespaceAndFormatting() throws IOException {
    String json = "{\n  \"key\" : \"value\",\n  \"nested\" : { \"a\" : 1 }\n}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    when(inputMessage.getHeaders()).thenReturn(headers);
    when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    String result = converter.read(ResolvableType.forClass(String.class), inputMessage, Map.of());

    assertEquals(json, result);
  }
}

