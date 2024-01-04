package org.folio.innreach.external.client.feign.error;

import feign.Request;
import feign.Response;
import io.github.glytching.junit.extension.random.Random;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Collections;

import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_GATEWAY_TIMEOUT;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

@ExtendWith(MockitoExtension.class)
class InnReachFeignErrorDecoderTest {

  @InjectMocks
  InnReachFeignErrorDecoder innReachFeignErrorDecoder;
  @Random
  private String methodKey;

  @ParameterizedTest
  @ValueSource(ints = {SC_BAD_GATEWAY, SC_GATEWAY_TIMEOUT})
  void testGateWayException(int status) {
    var exc = innReachFeignErrorDecoder.decode(methodKey, response(status));
    assertThat(exc, instanceOf(InnReachGatewayException.class));
  }

  @ParameterizedTest
  @ValueSource(ints = {SC_INTERNAL_SERVER_ERROR, SC_CONFLICT})
  void testInnReachException(int status) {
    var exc = innReachFeignErrorDecoder.decode(methodKey, response(status));
    assertThat(exc, instanceOf(InnReachException.class));
  }

  @Test
  void testBadCredentialsException() {
    var exc = innReachFeignErrorDecoder.decode(methodKey, response(SC_UNAUTHORIZED));
    assertThat(exc, instanceOf(BadCredentialsException.class));
  }

  @NotNull
  private Response response(int status) {
    return Response.builder()
      .status(status)
      .request(Request.create(
        Request.HttpMethod.GET, "", Collections.emptyMap(), (Request.Body) null, null))
      .build();
  }
}
