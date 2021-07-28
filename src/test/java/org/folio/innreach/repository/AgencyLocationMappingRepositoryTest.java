package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.folio.innreach.fixture.AgencyLocationMappingFixture.createLocalServerMapping;
import static org.folio.innreach.fixture.AgencyLocationMappingFixture.createMapping;
import static org.folio.innreach.fixture.AgencyLocationMappingFixture.findLocalServerMappingByCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql",
  "classpath:db/central-server/pre-populate-another-central-server.sql",
  "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
})
class AgencyLocationMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_MAPPING_ID = "b8d3f619-6a7c-41c3-9a99-bb937735dcc7";
  private static final String PRE_POPULATED_CS_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  private static final UUID PRE_POPULATED_LOCATION_UUID = fromString("99b0d4e2-a5ec-46a1-a5ea-1080e609f969");
  private static final UUID PRE_POPULATED_LIBRARY_UUID = fromString("70cf3473-77f2-4f5c-92c3-6489e65769e4");

  private static final String PRE_POPULATED_LOCAL_CODE = "5publ";

  private static final String PRE_POPULATED_USER = "admin";

  @Autowired
  private AgencyLocationMappingRepository repository;

  @Test
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(1, mappings.size());

    List<String> ids = mappings.stream()
      .map(mapping -> mapping.getId().toString())
      .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_MAPPING_ID));
  }

  @Test
  void shouldFetchExistingMapping() {
    var mapping = repository.fetchOneByCsId(fromString(PRE_POPULATED_CS_ID)).get();

    assertEquals(PRE_POPULATED_LIBRARY_UUID, mapping.getLibraryId());
    assertEquals(PRE_POPULATED_LOCATION_UUID, mapping.getLocationId());

    var localServerMappings = mapping.getLocalServerMappings();
    assertNotNull(localServerMappings);

    var foundLocalServerMapping = findLocalServerMappingByCode(mapping, PRE_POPULATED_LOCAL_CODE);
    assertNotNull(foundLocalServerMapping);

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getLastModifiedBy());
    assertNotNull(mapping.getLastModifiedDate());
  }

  @Test
  void shouldSaveNewMapping() {
    var newMapping = createMapping();

    var saved = repository.saveAndFlush(newMapping);

    var found = repository.getOne(saved.getId());
    assertNotNull(found);
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getLocationId(), found.getLocationId());
    assertEquals(saved.getLibraryId(), found.getLibraryId());
  }

  @Test
  void throwExceptionWhenSavingWithoutCentralServerRef() {
    var mapping = createMapping();
    mapping.setCentralServer(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void shouldUpdateLocationIdAndLibraryId() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING_ID));

    var newLocationId = randomUUID();
    var newLibraryId = randomUUID();
    mapping.setLocationId(newLocationId);
    mapping.setLibraryId(newLibraryId);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newLocationId, saved.getLocationId());
    assertEquals(newLibraryId, saved.getLibraryId());
  }

  @Test
  void shouldAddLocalServerMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING_ID));

    var newLsMapping = createLocalServerMapping();
    newLsMapping.setCentralServerMapping(mapping);
    mapping.getLocalServerMappings().add(newLsMapping);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());
    var savedLsMappings = saved.getLocalServerMappings();

    assertNotNull(savedLsMappings);
    assertTrue(savedLsMappings.contains(newLsMapping));
    assertEquals(2, savedLsMappings.size());
  }

  @Test
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_MAPPING_ID);

    repository.deleteById(id);

    Optional<AgencyLocationMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  void shouldDeleteLocalServerMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING_ID));

    var localServerMapping = findLocalServerMappingByCode(mapping, PRE_POPULATED_LOCAL_CODE);
    mapping.getLocalServerMappings().remove(localServerMapping);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());
    var savedLocalServerMappings = saved.getLocalServerMappings();

    assertFalse(savedLocalServerMappings.contains(localServerMapping));
  }

  @Test
  void throwExceptionWhenSavingWithoutLocationId() {
    var mapping = createMapping();
    mapping.setLocationId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithoutLibraryId() {
    var mapping = createMapping();
    mapping.setLibraryId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerId() {
    var mapping = createMapping();
    mapping.getCentralServer().setId(randomUUID());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_agency_location_mapping_cs_id]"));
  }

}
