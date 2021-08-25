package org.folio.innreach.domain.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.service.impl.KafkaService.EVENT_LISTENER_ID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @Mock
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @InjectMocks
  private KafkaService KafkaService;

  @Test
  void restartEventListeners() {
    var mockListenerContainer = mock(MessageListenerContainer.class);
    when(kafkaListenerEndpointRegistry.getListenerContainer(EVENT_LISTENER_ID)).thenReturn(mockListenerContainer);

    KafkaService.restartEventListeners();

    verify(mockListenerContainer).start();
    verify(mockListenerContainer).stop();
  }

}
