package org.folio.innreach.config;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;

import java.util.Collections;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.folio.spring.FolioExecutionContext;

@RequiredArgsConstructor
public class FolioRequestInterceptor implements RequestInterceptor {

  private final FolioExecutionContext folioExecutionContext;

  @Override
  @SneakyThrows
  public void apply(RequestTemplate template) {
    template.header(TOKEN, Collections.singletonList(folioExecutionContext.getToken()));
    template.header(TENANT, Collections.singletonList(folioExecutionContext.getTenantId()));
  }

}
