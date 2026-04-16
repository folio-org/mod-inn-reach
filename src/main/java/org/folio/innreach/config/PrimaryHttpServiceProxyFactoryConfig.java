package org.folio.innreach.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Marks the {@code httpServiceProxyFactory} bean (defined in folio-spring-base's
 * {@code HttpServiceClientConfiguration}) as primary.
 *
 * <p>This is needed because mod-inn-reach defines two additional
 * {@code HttpServiceProxyFactory} beans ({@code inventoryHttpServiceProxyFactory}
 * and {@code innReachHttpServiceProxyFactory}). When {@code folio.system-user.enabled=true},
 * the library class {@code OptionalSystemUserConfig} injects an unqualified
 * {@code HttpServiceProxyFactory} to create {@code AuthnClient}. Without a primary
 * bean, Spring cannot resolve the ambiguity among the three candidates.
 */
@Configuration
@ConditionalOnProperty(prefix = "folio.system-user", name = "enabled", havingValue = "true")
public class PrimaryHttpServiceProxyFactoryConfig implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    var beanDefinition = beanFactory.getBeanDefinition("httpServiceProxyFactory");
    beanDefinition.setPrimary(true);
  }
}

