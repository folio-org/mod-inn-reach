package org.folio.innreach.controller;


import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.dto.ValidationErrorDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.MaterialTypeMappingMapper;
import org.folio.innreach.repository.MaterialTypeMappingRepository;

@Sql(
  scripts = {
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MaterialTypeMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_MAPPING2_ID = "d9985d0d-b121-4ccd-ac16-5ebd0ccccf7f";
  private static final String PRE_POPULATED_MATERIAL_TYPE2_ID = "5ee11d91-f7e8-481d-b079-65d708582ccc";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MaterialTypeMappingRepository repository;
  @Autowired
  private MaterialTypeMappingMapper mapper;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldGetAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), MaterialTypeMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getMappings();

    List<MaterialTypeMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(entitiesToDTOs(dbMappings)));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void shouldGetEmptyMappingsWith0TotalIfNotSet() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), MaterialTypeMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldApplyLimitAndOffsetWhenGettingAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "?offset={offset}&limit={limit}",
          MaterialTypeMappingsDTO.class, Map.of("offset", 1, "limit", 1));

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var expectedMapping = findMapping(PRE_POPULATED_MAPPING2_ID);

    assertEquals(3, response.getTotalRecords());
    assertEquals(singletonList(expectedMapping), response.getMappings());
  }

  @Test
  void return400WhenGetAllExistingMappingsIfLimitAndOffsetInvalid() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "?offset={offset}&limit={limit}",
        ValidationErrorsDTO.class, Map.of("offset", -1, "limit", -1));

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    var errors = responseEntity.getBody();
    assertNotNull(errors);
    assertEquals(BAD_REQUEST.value(), errors.getCode());
    assertThat(collectFieldNames(errors), containsInAnyOrder(containsString("offset"), containsString("limit")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldGetSingleMappingById() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "/" + PRE_POPULATED_MAPPING2_ID,
        MaterialTypeMappingDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var mapping = responseEntity.getBody();
    assertNotNull(mapping);

    var expected = findMapping(PRE_POPULATED_MAPPING2_ID);

    assertEquals(expected, mapping);
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404WhenMappingIsNotFoundById() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "/" + UUID.randomUUID(),
        MaterialTypeMappingDTO.class);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewMapping() {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newMapping, MaterialTypeMappingDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertThat(created, samePropertyValuesAs(newMapping, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingAndCentralItemTypeIsNull() {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);
    newMapping.setCentralItemType(null);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newMapping, ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
        contains(createValidationError("centralItemType", "must not be null")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingAndMaterialTypeIdIsNull() {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);
    newMapping.setMaterialTypeId(null);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newMapping, ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
        contains(createValidationError("materialTypeId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void return400WhenCreatingNewMappingAndMaterialTypeIdAlreadyMapped() {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);
    newMapping.setMaterialTypeId(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE2_ID));

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newMapping, Error.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldUpdateExistingMapping() {
    var mapping = deserializeFromJsonFile("/material-type-mapping/update-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);

    var responseEntity = testRestTemplate.exchange(baseMappingURL() + "/{mappingId}", HttpMethod.PUT,
        new HttpEntity<>(mapping), MaterialTypeMappingDTO.class, PRE_POPULATED_MAPPING2_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfMappingNotFoundWhenUpdating() {
    var mapping = deserializeFromJsonFile("/material-type-mapping/update-material-type-mapping-request.json",
        MaterialTypeMappingDTO.class);

    var responseEntity = testRestTemplate.exchange(baseMappingURL() + "/{mappingId}", HttpMethod.PUT,
        new HttpEntity<>(mapping), MaterialTypeMappingDTO.class, UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldDeleteExistingMapping() {
    var responseEntity = testRestTemplate.exchange(baseMappingURL() + "/{mappingId}", HttpMethod.DELETE,
        HttpEntity.EMPTY, MaterialTypeMappingDTO.class, PRE_POPULATED_MAPPING2_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var deleted = repository.findById(UUID.fromString(PRE_POPULATED_MAPPING2_ID));
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfMappingNotFoundWhenDeleting() {
    var responseEntity = testRestTemplate.exchange(baseMappingURL() + "/{mappingId}", HttpMethod.DELETE,
        HttpEntity.EMPTY, MaterialTypeMappingDTO.class, UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/material-type-mappings";
  }

  private MaterialTypeMappingDTO[] entitiesToDTOs(List<MaterialTypeMapping> dbMappings) {
    MaterialTypeMappingDTO[] result = new MaterialTypeMappingDTO[dbMappings.size()];

    int i = 0;
    for (MaterialTypeMapping dbMapping : dbMappings) {
      result[i++] = mapper.toDTO(dbMapping);
    }

    return result;
  }

  private static List<String> collectFieldNames(ValidationErrorsDTO errors) {
    return errors.getValidationErrors().stream()
        .map(ValidationErrorDTO::getFieldName)
        .collect(Collectors.toList());
  }

  private MaterialTypeMappingDTO findMapping(String id) {
    var expectedEntity = repository.findById(UUID.fromString(id)).get();

    return mapper.toDTO(expectedEntity);
  }

  private ValidationErrorDTO createValidationError(String field, String message) {
    var result = new ValidationErrorDTO();

    result.setFieldName(field);
    result.setMessage(message);

    return result;
  }

}
