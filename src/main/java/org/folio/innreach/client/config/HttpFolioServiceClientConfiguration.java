package org.folio.innreach.client.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@ConditionalOnProperty(prefix = "folio.exchange", name = "enabled", havingValue = "true")
public class HttpFolioServiceClientConfiguration {

  @Bean("inventoryHttpServiceProxyFactory")
  public HttpServiceProxyFactory inventoryHttpServiceProxyFactory(RestClient.Builder restClientBuilder,
                                                                  InventoryErrorHandler errorHandler) {
    var client = restClientBuilder
      .defaultStatusHandler(this::isConflictOrServerError,
        ((request, response) -> errorHandler.handle(response)))
      .build();

    return HttpServiceProxyFactory
      .builderFor(RestClientAdapter.create(client))
      .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
      .build();
  }

  private boolean isConflictOrServerError(HttpStatusCode statusCode) {
    return statusCode.value() == HttpStatus.CONFLICT.value() || statusCode.is5xxServerError();
  }
}
