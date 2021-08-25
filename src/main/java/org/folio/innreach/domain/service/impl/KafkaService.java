package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaService {

  public static final String EVENT_LISTENER_ID = "mod-innreach-events-listener";

  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  public void restartEventListeners() {
    log.info("Restarting kafka listener [id: {}]", EVENT_LISTENER_ID);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(EVENT_LISTENER_ID);
    listenerContainer.stop();
    listenerContainer.start();
  }

}
