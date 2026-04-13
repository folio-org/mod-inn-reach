package org.folio.innreach.external.client.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
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
    var stringConverter = new StringHttpMessageConverter();
    stringConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.ALL));

    return RestClient.builder()
      .defaultStatusHandler(HttpStatusCode::isError,
        ((request, response) -> errorHandler.handle(response)))
      .configureMessageConverters(configurer ->
        configurer
          .addCustomConverter(stringConverter)
          .addCustomConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
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
