package org.folio.innreach.external.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(prefix = "folio.exchange", name = "enabled", havingValue = "true")
public class HttpInnReachServiceClientConfiguration {

  @Bean("innReachRestClientBuilder")
  public RestClient.Builder innReachRestClientBuilder(InnReachErrorHandler errorHandler, JsonMapper jsonMapper) {
    return RestClient.builder()
      .defaultStatusHandler(HttpStatusCode::isError,
        ((request, response) -> errorHandler.handle(response)))
      .configureMessageConverters(configurer -> configurer
        .registerDefaults()
        .withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
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
}
