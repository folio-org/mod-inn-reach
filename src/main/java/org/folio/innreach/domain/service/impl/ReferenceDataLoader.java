package org.folio.innreach.domain.service.impl;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
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

  public void loadRefData() {
    try {
      log.info("Loading reference data");
      loadInstanceTypes();
      loadContributorNameTypes();
    } catch (Exception e) {
      log.warn("Unable to load reference data", e);
      throw new IllegalStateException("Unable to load reference data", e);
    }
  }

  private void loadInstanceTypes() throws IOException {
    log.info("Loading instance types");
    for (var res : getResources(INSTANCE_TYPES_DIR)) {
      var instanceType = jsonHelper.fromJson(res.getInputStream(), InstanceTypeClient.InstanceType.class);

      var existing = instanceTypeClient.getInstanceTypeByName(instanceType.getName());
      if (existing.isEmpty()) {
        log.info("Creating instance type {}", instanceType);
        instanceTypeClient.createInstanceType(instanceType);
      }
    }
  }

  private void loadContributorNameTypes() throws IOException {
    log.info("Loading contributor name types");

    for (var res : getResources(CONTRIBUTION_NAME_TYPES_DIR)) {
      var contributorType = jsonHelper.fromJson(res.getInputStream(), InstanceContributorTypeClient.NameType.class);

      var existing = instanceContributorTypeClient.getContributorType(contributorType.getName());
      if (existing.isEmpty()) {
        log.info("Creating contributor name type {}", contributorType);
        instanceContributorTypeClient.createNameType(contributorType);
      }
    }
  }

  private static Resource[] getResources(String resourceDir) throws IOException {
    return resourcePatternResolver.getResources(String.format("/%s/%s/%s", BASE_DIR, resourceDir, "*.json"));
  }
}
