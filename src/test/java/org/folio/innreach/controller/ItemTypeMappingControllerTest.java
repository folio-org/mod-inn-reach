package org.folio.innreach.controller;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomIntegerExcept;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.ItemTypeMappingDTO;
import org.folio.innreach.dto.ItemTypeMappingsDTO;
import org.folio.innreach.mapper.ItemTypeMappingMapper;
import org.folio.innreach.repository.ItemTypeMappingRepository;

@Sql(
  scripts = {
    "classpath:db/item-type-mapping/clear-item-type-mapping-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ItemTypeMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID1 = "f8c5d329-c3db-40c1-9e96-d6176f76b0da";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID2 = "606f5a38-9e1f-45d0-856e-899c5667410d";

  private static final Integer PRE_POPULATED_CENTRAL_ITEM_TYPE1 = 1;

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ItemTypeMappingRepository repository;
  @Autowired
  private ItemTypeMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void shouldGetAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-type-mappings", ItemTypeMappingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getItemTypeMappings();

    List<ItemTypeMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void shouldUpdateAllExistingMappings() {
    var existing = mapper.toDTOCollection(repository.findAll());
    var centralItemTypes = existing.getItemTypeMappings().stream().map(
      ItemTypeMappingDTO::getCentralItemType).collect(Collectors.toSet());
    centralItemTypes.forEach(t -> t = randomIntegerExcept(256, centralItemTypes));
    var updatedCentralItemTypes = List.copyOf(centralItemTypes);
    existing.getItemTypeMappings().forEach(m -> m.setCentralItemType(updatedCentralItemTypes.get(existing.getItemTypeMappings().indexOf(m))));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/item-type-mappings", HttpMethod.PUT,
      new HttpEntity<>(existing), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTOs(repository.findAll());
    var expected = existing.getItemTypeMappings();

    assertEquals(expected.size(), updated.size());
    assertEquals(expected.stream().map(ItemTypeMappingDTO::getCentralItemType).collect(Collectors.toList()),
      updated.stream().map(ItemTypeMappingDTO::getCentralItemType).collect(Collectors.toList()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() {
    var mappings = mapper.toDTOCollection(repository.findAll());
    List<ItemTypeMappingDTO> em = mappings.getItemTypeMappings();

    em.removeIf(idEqualsTo(fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1)));         // to delete
    Integer centralItemType = randomIntegerExcept(256, Set.of(1, 2));
    findInList(em, fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID2))   // to update
      .ifPresent(mapping -> mapping.setCentralItemType(centralItemType));

    var newMappings = deserializeFromJsonFile("/item-type-mapping/create-item-type-mappings-request.json",
      ItemTypeMappingsDTO.class);
    em.addAll(newMappings.getItemTypeMappings());       // to insert

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/item-type-mappings", HttpMethod.PUT, new HttpEntity<>(mappings),
      Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var stored = mapper.toDTOs(repository.findAll());

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1)).isEmpty());
    // verify updated
    assertEquals(centralItemType,
      findInList(stored, fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID2))
        .map(ItemTypeMappingDTO::getCentralItemType).get());
    // verify inserted
    assertThat(stored, hasItems(
      samePropertyValuesAs(newMappings.getItemTypeMappings().get(0), "id", "metadata"),
      samePropertyValuesAs(newMappings.getItemTypeMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void return409WhenUpdatingMappingAndCentralItemTypeIsAlreadyMapped() {
    var existing = mapper.toDTOCollection(repository.findAll());
    existing.getItemTypeMappings().forEach(m -> m.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE1));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/item-type-mappings", HttpMethod.PUT, new HttpEntity<>(existing),
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void return409WhenUpdatingMappingWithInvalidCentralItemType() {
    var newMapping = deserializeFromJsonFile("/item-type-mapping/update-item-type-mappings-invalid-request.json",
      ItemTypeMappingsDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/item-type-mappings", HttpMethod.PUT, new HttpEntity<>(newMapping),
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
  }

  private static Predicate<ItemTypeMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static Optional<ItemTypeMappingDTO> findInList(List<ItemTypeMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }
}
