package org.folio.innreach.domain.service.impl;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceContributorTypeClient.NameType;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.InstanceTypeClient.InstanceType;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.util.JsonHelper;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReferenceDataLoader {

  private static final String BASE_DIR = "reference-data";
  private static final String INSTANCE_TYPES_DIR = "instance-types";
  private static final String CONTRIBUTION_NAME_TYPES_DIR = "contributor-name-types";

  private static final PathMatchingResourcePatternResolver resourcePatternResolver =
    new PathMatchingResourcePatternResolver(ReferenceDataLoader.class.getClassLoader());

  private final InstanceContributorTypeClient instanceContributorTypeClient;
  private final InstanceTypeClient instanceTypeClient;
  private final JsonHelper jsonHelper;

  @Async
  @Retryable(maxAttemptsExpression = "#{${reference-data.loader.retry-attempts}}",
    backoff = @Backoff(delayExpression = "#{${reference-data.loader.retry-interval-ms}}"))
  public void loadRefData() {
    try {
      log.info("Loading reference data");
      loadInstanceTypes();
      loadContributorNameTypes();
    } catch (Exception e) {
      log.warn("Unable to load reference data", e);
      throw new IllegalStateException("Unable to load reference data", e);
    }
    log.info("Finished loading reference data");
  }

  private void loadInstanceTypes() {
    load(INSTANCE_TYPES_DIR, InstanceType.class,
      r -> instanceTypeClient.queryInstanceTypeByName(r.getName()),
      r -> instanceTypeClient.createInstanceType(r)
    );
  }

  private void loadContributorNameTypes() {
    load(CONTRIBUTION_NAME_TYPES_DIR, NameType.class,
      r -> instanceContributorTypeClient.queryContributorTypeByName(r.getName()),
      r -> instanceContributorTypeClient.createContributorType(r)
    );
  }

  private <T> void load(String resourceDir, Class<T> resourceType,
                        Function<T, ResultList<T>> loadExistingFunc,
                        Consumer<T> createFunc) {
    for (var res : getResources(resourceDir)) {
      T rec = deserializeRecord(resourceType, res);

      var existing = loadExistingFunc.apply(rec);

      if (isEmpty(existing.getResult())) {
        log.info("Creating reference data record {}", rec);
        createFunc.accept(rec);
      }
    }
  }

  private <T> T deserializeRecord(Class<T> resourceType, Resource res) {
    try {
      return jsonHelper.fromJson(res.getInputStream(), resourceType);
    } catch (IOException e) {
      var msg = String.format("Failed to deserialize reference data of type %s from file: %s", resourceType, res.getFilename());
      throw new IllegalStateException(msg, e);
    }
  }

  private static Resource[] getResources(String resourceDir) {
    try {
      return resourcePatternResolver.getResources(String.format("/%s/%s/%s", BASE_DIR, resourceDir, "*.json"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load reference data on path: " + resourceDir, e);
    }
  }
}
