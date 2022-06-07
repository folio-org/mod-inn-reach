package org.folio.innreach.config;


import org.springframework.beans.factory.InitializingBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spring.web.WebMvcRequestHandler;
import springfox.documentation.spring.web.paths.Paths;
import springfox.documentation.spring.web.plugins.DocumentationPluginsBootstrapper;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import javax.servlet.ServletContext;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static springfox.documentation.spi.service.contexts.Orderings.byPatternsCondition;

@Configuration
public class SpringUtilConfig {

  @Bean
  public InitializingBean removeSpringfoxHandlerProvider(DocumentationPluginsBootstrapper bootstrapper) {
    return () -> bootstrapper.getHandlerProviders().removeIf(WebMvcRequestHandlerProvider.class::isInstance);
  }

  @Bean
  public RequestHandlerProvider customRequestHandlerProvider(Optional<ServletContext> servletContext, HandlerMethodResolver methodResolver, List<RequestMappingInfoHandlerMapping> handlerMappings) {
    String contextPath = servletContext.map(ServletContext::getContextPath).orElse(Paths.ROOT);
    return () -> handlerMappings.stream()
      .filter(mapping -> !mapping.getClass().getSimpleName().equals("IntegrationRequestMappingHandlerMapping"))
      .map(mapping -> mapping.getHandlerMethods().entrySet())
      .flatMap(Set::stream)
      .map(entry -> new WebMvcRequestHandler(contextPath, methodResolver, tweakInfo(entry.getKey()), entry.getValue()))
      .sorted(byPatternsCondition())
      .collect(toList());
  }

  RequestMappingInfo tweakInfo(RequestMappingInfo info) {
    if (info.getPathPatternsCondition() == null) return info;
    String[] patterns = null;
      Optional<PathPatternsRequestCondition> requestCondition = Optional.of(info.getPathPatternsCondition());
    if (requestCondition.isPresent()) {
      patterns = requestCondition.get().getPatternValues().toArray(String[]::new);
    }
    return info.mutate().options(new RequestMappingInfo.BuilderConfiguration()).paths(patterns).build();
  }

}
