package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createLocationMapping;
import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.MappingFixture.refInnReachLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.LocationMapping;

@Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql"
})
class LocationMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_MAPPING1_ID = "bea259f7-dce0-41de-8c31-3ae6e3034840";
  private static final String PRE_POPULATED_MAPPING2_ID = "b4262548-3e38-424c-b3d9-509af233db5f";
  private static final String PRE_POPULATED_MAPPING3_ID = "ada69896-3954-45dc-92cb-04182afb2548";

  private static final UUID PRE_POPULATED_LOCATION1_UUID = fromString("ae937212-5e3f-4ca4-8f1e-1aa2d83bb295");
  private static final UUID PRE_POPULATED_LIBRARY_UUID = fromString("a0dd1106-3de8-4346-b0f4-b1ed0a4eaffd");
  private static final UUID PRE_POPULATED_IR_LOCATION1_UUID = fromString("a1c1472f-67ec-4938-b5a8-f119e51ab79b");
  private static final UUID PRE_POPULATED_IR_LOCATION2_UUID = fromString("26f7c8c5-f090-4742-b7c7-e08ed1cc4e67");
  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");

  private static final String PRE_POPULATED_USER = "admin";

  @Autowired
  private LocationMappingRepository repository;


  @Test
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(3, mappings.size());

    List<String> ids = mappings.stream()
        .map(mapping -> mapping.getId().toString())
        .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_MAPPING1_ID, PRE_POPULATED_MAPPING2_ID, PRE_POPULATED_MAPPING3_ID));
  }

  @Test
  void shouldGetExistingMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    assertEquals(PRE_POPULATED_LOCATION1_UUID, mapping.getLocationId());
    assertEquals(PRE_POPULATED_LIBRARY_UUID, mapping.getLibraryId());
    assertEquals(PRE_POPULATED_IR_LOCATION1_UUID, mapping.getInnReachLocation().getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_UUID, mapping.getCentralServer().getId());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getLastModifiedBy());
    assertNotNull(mapping.getLastModifiedDate());
  }

  @Test
  void shouldSaveNewMapping() {
    var newMapping = createLocationMapping();

    var saved = repository.saveAndFlush(newMapping);

    LocationMapping found = repository.getOne(saved.getId());
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getLocationId(), found.getLocationId());
    assertEquals(saved.getLibraryId(), found.getLibraryId());
    assertEquals(saved.getCentralServer().getId(), found.getCentralServer().getId());
    assertEquals(saved.getInnReachLocation().getId(), found.getInnReachLocation().getId());
  }

  @Test
  void throwExceptionWhenSavingWithoutId() {
    var mapping = createLocationMapping();
    mapping.setId(null);

    assertThrows(JpaSystemException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void shouldUpdateLocationIdLibraryIdAndIRLocationId() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    UUID newLocationId = randomUUID();
    UUID newLibraryId = randomUUID();
    UUID newIRLocationId = PRE_POPULATED_IR_LOCATION2_UUID;
    mapping.setLocationId(newLocationId);
    mapping.setLibraryId(newLibraryId);
    mapping.setInnReachLocation(refInnReachLocation(newIRLocationId));

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newLocationId, saved.getLocationId());
    assertEquals(newLibraryId, saved.getLibraryId());
    assertEquals(newIRLocationId, saved.getInnReachLocation().getId());
  }

  @Test
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_MAPPING1_ID);

    repository.deleteById(id);

    Optional<LocationMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  void throwExceptionWhenSavingWithoutLocationId() {
    var mapping = createLocationMapping();
    mapping.setLocationId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithoutLibraryId() {
    var mapping = createLocationMapping();
    mapping.setLibraryId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createLocationMapping();

    mapping.setCentralServer(refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_location_mapping_central_server]"));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidInnReachLocationReference() {
    var mapping = createLocationMapping();

    mapping.setInnReachLocation(refInnReachLocation(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_location_mapping_ir_location]"));
  }

  @Test
  void throwExceptionWhenNewLocationMappingExistsForTheServer() {
    var existing = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    var mapping = createLocationMapping();
    mapping.setLocationId(existing.getLocationId());
    mapping.setInnReachLocation(existing.getInnReachLocation());
    mapping.setCentralServer(existing.getCentralServer());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_location_mapping_server_loc_irloc]"));
  }

}
