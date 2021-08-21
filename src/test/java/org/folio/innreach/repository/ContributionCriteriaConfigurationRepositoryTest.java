package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.ContributionCriteriaConfigurationFixture.createContributionCriteriaConfiguration;
import static org.folio.innreach.fixture.TestUtil.refCentralServer;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;

@Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
@Sql(scripts = "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql")
class ContributionCriteriaConfigurationRepositoryTest extends BaseRepositoryTest {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER1_UUID = fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_CENTRAL_SERVER2_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");
  private static final UUID PRE_POPULATED_CRITERIA_ID = fromString("71bd0beb-28cb-40bb-9f40-87463d61a553");
  private static final UUID PRE_POPULATED_SUPPRESS_CODE_ID = fromString("8d87682b-0414-4e1a-b810-43df2cda69d1");
  private static final UUID PRE_POPULATED_SYSTEM_OWNED_CODE_ID = fromString("7ee055ce-64b3-4e12-9253-f56762412a7e");
  private static final UUID PRE_POPULATED_NOT_CONTRIBUTE_CODE_ID = fromString("5599f23f-d424-4fce-8a51-b7fce690cbda");
  private static final String PRE_POPULATED_USER = "admin";

  @Autowired
  private ContributionCriteriaConfigurationRepository repository;

  @Test
  void shouldFindExistingCriteriaByServerId() {
    var found = repository.findOne(exampleWithServerId(PRE_POPULATED_CENTRAL_SERVER1_UUID));

    assertTrue(found.isPresent());
    var criteria = found.get();

    assertEquals(PRE_POPULATED_CRITERIA_ID, criteria.getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER1_UUID, criteria.getCentralServer().getId());
    assertEquals(PRE_POPULATED_SUPPRESS_CODE_ID, criteria.getContributeButSuppressCodeId());
    assertEquals(PRE_POPULATED_SYSTEM_OWNED_CODE_ID, criteria.getContributeAsSystemOwnedCodeId());
    assertEquals(PRE_POPULATED_NOT_CONTRIBUTE_CODE_ID, criteria.getDoNotContributeCodeId());
    assertEquals(3, criteria.getExcludedLocationIds().size());
  }

  @Test
  void shouldGetCriteriaWithMetadata() {
    var mapping = repository.getOne(PRE_POPULATED_CRITERIA_ID);

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getUpdatedBy());
    assertNotNull(mapping.getUpdatedDate());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-another-central-server.sql")
  void shouldSaveNewCriteria() {
    var criteria = createContributionCriteriaConfiguration();
    criteria.setCentralServer(refCentralServer(PRE_POPULATED_CENTRAL_SERVER2_UUID));

    var saved = repository.saveAndFlush(criteria);

    ContributionCriteriaConfiguration found = repository.getOne(saved.getId());
    assertTrue(equalsTo(found, saved));
  }

  @Test
  void shouldUpdateCodeIds() {
    var criteria = repository.getOne(PRE_POPULATED_CRITERIA_ID);

    UUID notContributeCodeId = randomUUID();
    criteria.setDoNotContributeCodeId(notContributeCodeId);
    UUID suppressCodeId = randomUUID();
    criteria.setContributeButSuppressCodeId(suppressCodeId);
    UUID systemOwnedCodeId = randomUUID();
    criteria.setContributeAsSystemOwnedCodeId(systemOwnedCodeId);

    repository.saveAndFlush(criteria);

    var saved = repository.getOne(criteria.getId());

    assertEquals(notContributeCodeId, saved.getDoNotContributeCodeId());
    assertEquals(suppressCodeId, saved.getContributeButSuppressCodeId());
    assertEquals(systemOwnedCodeId, saved.getContributeAsSystemOwnedCodeId());
  }

  @Test
  void shouldRemoveCodeIds() {
    var criteria = repository.getOne(PRE_POPULATED_CRITERIA_ID);

    criteria.setDoNotContributeCodeId(null);
    criteria.setContributeButSuppressCodeId(null);
    criteria.setContributeAsSystemOwnedCodeId(null);

    repository.saveAndFlush(criteria);

    var saved = repository.getOne(criteria.getId());

    assertNull(saved.getDoNotContributeCodeId());
    assertNull(saved.getContributeButSuppressCodeId());
    assertNull(saved.getContributeAsSystemOwnedCodeId());
  }

  @Test
  void shouldAddExcludedLocationIds() {
    var criteria = repository.getOne(PRE_POPULATED_CRITERIA_ID);

    UUID location1 = randomUUID();
    criteria.addExcludedLocationId(location1);
    UUID location2 = randomUUID();
    criteria.addExcludedLocationId(location2);

    var expected = new ArrayList<>(criteria.getExcludedLocationIds());

    repository.saveAndFlush(criteria);

    var saved = repository.getOne(criteria.getId());
    var savedLocationIds = saved.getExcludedLocationIds();

    assertThat(savedLocationIds, containsInAnyOrder(expected.toArray()));
  }

  @Test
  void shouldRemoveExcludedLocationIds() {
    var criteria = repository.getOne(PRE_POPULATED_CRITERIA_ID);

    UUID location1 = criteria.getExcludedLocationIds().get(0);
    UUID location2 = criteria.getExcludedLocationIds().get(1);
    criteria.removeExcludedLocationId(location1);
    criteria.removeExcludedLocationId(location2);

    var expected = new ArrayList<>(criteria.getExcludedLocationIds());

    repository.saveAndFlush(criteria);

    var saved = repository.getOne(criteria.getId());
    var savedLocationIds = saved.getExcludedLocationIds();

    assertThat(savedLocationIds, containsInAnyOrder(expected.toArray()));
  }

  @Test
  void shouldDeleteExistingCriteria() {
    repository.deleteById(PRE_POPULATED_CRITERIA_ID);

    Optional<ContributionCriteriaConfiguration> deleted = repository.findById(PRE_POPULATED_CRITERIA_ID);
    assertTrue(deleted.isEmpty());
  }

  @Test
  void throwExceptionWhenSavingWithoutId() {
    var criteria = createContributionCriteriaConfiguration();
    criteria.setId(null);

    assertThrows(JpaSystemException.class, () -> repository.saveAndFlush(criteria));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var criteria = createContributionCriteriaConfiguration();

    criteria.setCentralServer(refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(criteria));
    assertThat(ex.getMessage(), containsString("constraint [fk_contribution_criteria_central_server]"));
  }

  @Test
  void throwExceptionWhenSavingWithAlreadyUsedCentralServerReference() {
    var criteria = createContributionCriteriaConfiguration();

    criteria.setCentralServer(refCentralServer(PRE_POPULATED_CENTRAL_SERVER1_UUID));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(criteria));
    assertThat(ex.getMessage(), containsString("constraint [unq_contribution_criteria_server]"));
  }

  private static Example<ContributionCriteriaConfiguration> exampleWithServerId(UUID centralServerId) {
    var toFind = new ContributionCriteriaConfiguration();

    toFind.setCentralServer(refCentralServer(centralServerId));

    return Example.of(toFind);
  }

  private static boolean equalsTo(ContributionCriteriaConfiguration a, ContributionCriteriaConfiguration b) {
    return (a != null && a.equals(b)) &&
        Objects.equals(a.getContributeButSuppressCodeId(), b.getContributeButSuppressCodeId()) &&
        Objects.equals(a.getContributeAsSystemOwnedCodeId(), b.getContributeAsSystemOwnedCodeId()) &&
        Objects.equals(a.getDoNotContributeCodeId(), b.getDoNotContributeCodeId()) &&
        Objects.equals(a.getCentralServer(), b.getCentralServer()) &&
        Objects.equals(a.getExcludedLocationIds(), b.getExcludedLocationIds());
  }

}
