package org.folio.innreach.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

class PrimaryHttpServiceProxyFactoryConfigTest {

  private final PrimaryHttpServiceProxyFactoryConfig config = new PrimaryHttpServiceProxyFactoryConfig();

  @Test
  void markHttpServiceProxyFactoryAsPrimary() {
    var beanFactory = mock(ConfigurableListableBeanFactory.class);
    var beanDefinition = mock(BeanDefinition.class);
    when(beanFactory.getBeanDefinition("httpServiceProxyFactory")).thenReturn(beanDefinition);

    config.postProcessBeanFactory(beanFactory);

    verify(beanDefinition).setPrimary(true);
  }

  @Test
  void lookUpCorrectBeanName() {
    var beanFactory = mock(ConfigurableListableBeanFactory.class);
    var beanDefinition = mock(BeanDefinition.class);
    when(beanFactory.getBeanDefinition("httpServiceProxyFactory")).thenReturn(beanDefinition);

    config.postProcessBeanFactory(beanFactory);

    verify(beanFactory).getBeanDefinition("httpServiceProxyFactory");
  }

  @Test
  void propagateExceptionWhenBeanNotFound() {
    var beanFactory = mock(ConfigurableListableBeanFactory.class);
    when(beanFactory.getBeanDefinition("httpServiceProxyFactory"))
        .thenThrow(new NoSuchBeanDefinitionException("httpServiceProxyFactory"));

    assertThatThrownBy(() -> config.postProcessBeanFactory(beanFactory))
        .isInstanceOf(NoSuchBeanDefinitionException.class);
  }

  @Test
  void doNotModifyOtherBeanDefinitions() {
    var beanFactory = mock(ConfigurableListableBeanFactory.class);
    var targetDefinition = mock(BeanDefinition.class);
    var otherDefinition = mock(BeanDefinition.class);
    when(beanFactory.getBeanDefinition("httpServiceProxyFactory")).thenReturn(targetDefinition);

    config.postProcessBeanFactory(beanFactory);

    verify(otherDefinition, never()).setPrimary(true);
  }
}
