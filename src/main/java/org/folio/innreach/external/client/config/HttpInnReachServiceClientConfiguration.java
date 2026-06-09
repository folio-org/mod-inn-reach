package org.folio.innreach.external.client.config;

import org.folio.innreach.config.props.InnReachHttpClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;

@Configuration
@ConditionalOnProperty(prefix = "folio.exchange", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(InnReachHttpClientProperties.class)
public class HttpInnReachServiceClientConfiguration {

  private final InnReachHttpClientProperties innReachHttpClientProperties;

  public HttpInnReachServiceClientConfiguration(InnReachHttpClientProperties innReachHttpClientProperties) {
    this.innReachHttpClientProperties = innReachHttpClientProperties;
  }

  @Bean("innReachRestClientBuilder")
  public RestClient.Builder innReachRestClientBuilder(InnReachErrorHandler errorHandler, JsonMapper jsonMapper) {
    var jacksonJsonMsgConverter = new JacksonJsonHttpMessageConverter(jsonMapper);
    var supportedMediaTypes = new ArrayList<>(jacksonJsonMsgConverter.getSupportedMediaTypes());
    supportedMediaTypes.add(new MediaType("text", "json"));
    jacksonJsonMsgConverter.setSupportedMediaTypes(supportedMediaTypes);

    return RestClient.builder()
      .requestFactory(buildRequestFactory())
      .defaultStatusHandler(HttpStatusCode::isError,
        ((request, response) -> errorHandler.handle(response)))
      .configureMessageConverters(configurer -> configurer
        .registerDefaults()
        .withJsonConverter(jacksonJsonMsgConverter)
        .configureMessageConvertersList(converters -> converters.addFirst(new RawJsonStringHttpMessageConverter()))
      );
  }

  @Bean("innReachHttpServiceProxyFactory")
  public HttpServiceProxyFactory innReachHttpServiceProxyFactory(@Qualifier("innReachRestClientBuilder") RestClient.Builder innReachRestClientBuilder) {
    return HttpServiceProxyFactory
      .builderFor(RestClientAdapter.create(innReachRestClientBuilder.build()))
      .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
      .build();
  }

  private ClientHttpRequestFactory buildRequestFactory() {
    var httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofMillis(innReachHttpClientProperties.connectTimeoutMs()))
      .build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(innReachHttpClientProperties.readTimeoutMs()));

    return requestFactory;
  }
}
