package org.folio.innreach.client.customization;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CONTINUE;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import org.apache.commons.lang3.RandomUtils;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.customization.InventoryErrorDecoder.InventoryError;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.util.JsonHelper;

@ExtendWith(MockitoExtension.class)
@ExtendWith(RandomBeansExtension.class)
class InventoryErrorDecoderTest {

  private static final String ERR_MSG = "Cannot update record because it has been changed (optimistic locking): " +
      "Stored _version is 2, _version of request is 1";
  private static final String ERR_SEVERITY = "ERROR";
  private static final String ERR_CODE = "23F09";

  @Mock
  private ErrorDecoder defaultDecoder;
  @Mock
  private JsonHelper jsonHelper;
  @InjectMocks
  private InventoryErrorDecoder decoder;
  @Random
  private String methodKey;
  private final Exception defaultException = new Exception();


  @ParameterizedTest
  @ValueSource(ints = {SC_CONFLICT, SC_INTERNAL_SERVER_ERROR})
  void returnConflictException_when_Http409OrHttp500_and_ConflictErrorInResponse(int status) throws IOException {
    Response response = response(status);

    mockErrorParsing(response, conflictError());

    var exc = decoder.decode(methodKey, response);

    assertThat(exc, instanceOf(ResourceVersionConflictException.class));
    assertThat(exc.getMessage(), is(ERR_MSG));
  }

  @ParameterizedTest
  @ValueSource(ints = {SC_CONFLICT, SC_INTERNAL_SERVER_ERROR})
  void returnDefaultException_when_Http409OrHttp500_but_NoConflictErrorInResponse(int status) throws IOException {
    Response response = response(status);

    mockErrorParsing(response, randomError());
    mockDefaultDecoder(response, defaultException);

    var exc = decoder.decode(methodKey, response);

    assertThat(exc, is(defaultException));
  }

  @ParameterizedTest
  @MethodSource("notHttp409OrHttp500Provider")
  void returnDefaultException_when_NotHttp409OrHttp500(int status) {
    Response response = response(status);

    mockDefaultDecoder(response, defaultException);

    var exc = decoder.decode(methodKey, response);

    assertThat(exc, is(defaultException));
  }

  @ParameterizedTest
  @MethodSource("parsingExceptionProvider")
  void returnDefaultException_when_ResponseParsingFailed(Exception exception) throws IOException {
    Response response = response(RandomUtils.nextInt());

    mockErrorParsing(response, exception);
    mockDefaultDecoder(response, defaultException);

    var exc = decoder.decode(methodKey, response);

    assertThat(exc, is(defaultException));
  }

  public static IntStream notHttp409OrHttp500Provider() {
    return IntStream.range(SC_CONTINUE, SC_INSUFFICIENT_STORAGE)
        .filter(value -> value != SC_CONFLICT && value != SC_INTERNAL_SERVER_ERROR);
  }

  public static Stream<Exception> parsingExceptionProvider() {
    return Stream.of(new IOException(), new IllegalStateException());
  }

  private void mockDefaultDecoder(Response response, Exception defaultException) {
    when(defaultDecoder.decode(methodKey, response)).thenReturn(defaultException);
  }

  @NotNull
  private static InventoryError conflictError() {
    return new InventoryError(ERR_MSG, ERR_SEVERITY, ERR_CODE);
  }

  private static InventoryError randomError() {
    return new InventoryError(randomString(), randomString(), randomString());
  }

  private void mockErrorParsing(Response response, InventoryError inventoryError) throws IOException {
    when(jsonHelper.fromJson(response.body().asInputStream(), InventoryError.class)).thenReturn(inventoryError);
  }

  private void mockErrorParsing(Response response, Exception exception) throws IOException {
    lenient().when(jsonHelper.fromJson(response.body().asInputStream(), InventoryError.class)).thenThrow(exception);
  }

  private static String randomString() {
    return new StringRandomizer().getRandomValue();
  }

  @NotNull
  private Response response(int status) {
    byte[] data = RandomUtils.nextBytes(50);

    return Response.builder()
        .status(status)
        .body(new ByteArrayInputStream(data), data.length)
        .request(Request.create(
            Request.HttpMethod.PUT, randomString(), Collections.emptyMap(), (Request.Body) null, null))
        .build();
  }

}
