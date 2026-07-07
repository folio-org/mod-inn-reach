package org.folio.innreach.support.kafka;

import static org.folio.innreach.domain.listener.KafkaListenersConstants.CIRC_CHECKIN_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.CIRC_LOAN_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.CIRC_REQUEST_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INITIAL_CONTRIBUTION_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_HOLDING_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_INSTANCE_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC1;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC2;
import static org.testcontainers.utility.DockerImageName.parse;

import java.util.List;
import java.util.Properties;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Log4j2
public class KafkaContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String SPRING_PROPERTY_NAME = "spring.kafka.bootstrap-servers";
  private static final DockerImageName KAFKA_IMAGE = parse("apache/kafka-native:3.8.0");
  private static final KafkaContainer CONTAINER = new KafkaContainer(KAFKA_IMAGE)
    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
    .withStartupAttempts(3);

  private static final List<String> DEFAULT_TOPICS = List.of(
    CIRC_LOAN_TOPIC,
    CIRC_REQUEST_TOPIC,
    CIRC_CHECKIN_TOPIC,
    INVENTORY_ITEM_TOPIC,
    INVENTORY_HOLDING_TOPIC,
    INVENTORY_INSTANCE_TOPIC,
    INITIAL_CONTRIBUTION_TOPIC,
    INVENTORY_ITEM_TOPIC1,
    INVENTORY_ITEM_TOPIC2
  );

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_PROPERTY_NAME, CONTAINER.getBootstrapServers());
    createTopics(DEFAULT_TOPICS);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_PROPERTY_NAME);
  }

  @SneakyThrows
  public static void createTopics(List<String> topicNames) {
    var newTopics = topicNames.stream()
      .map(topicName -> new NewTopic(topicName, 1, (short) 1))
      .toList();

    log.info("Creating topics: {}", newTopics);
    try (var adminClient = getAdminClient()) {
      adminClient.createTopics(newTopics);
    }
  }

  @SneakyThrows
  public static void deleteTopics(List<String> topicNames) {
    try (var adminClient = getAdminClient()) {
      var existingTopics = adminClient.listTopics().names().get();
      var topicsToDelete = topicNames.stream()
        .filter(existingTopics::contains)
        .toList();

      if (!topicsToDelete.isEmpty()) {
        log.info("Removing Kafka topics: {}", topicsToDelete);
        adminClient.deleteTopics(topicsToDelete);
      } else {
        log.debug("No Kafka topics to delete from: {}", topicNames);
      }
    }
  }

  public static AdminClient getAdminClient() {
    var props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
    return KafkaAdminClient.create(props);
  }

  public static String getBootstrapServers() {
    if (!CONTAINER.isRunning()) {
      log.error("Kafka container is not running. Returning empty bootstrap servers.");
      return "";
    }
    return CONTAINER.getBootstrapServers();
  }
}
