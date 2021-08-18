package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.MaterialTypeDTO;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionRepository;

@Getter
@Sql(
  scripts = {
    "classpath:db/contribution/clear-contribution-tables.sql",
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
    "classpath:db/lib-mapping/clear-library-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ContributionControllerTest extends BaseControllerTest {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final String PRE_POPULATED_TYPE_ID = "1a54b431-2e4f-452d-9cae-9cee66c9a892";
  private static final String PRE_POPULATED_TYPE2_ID = "5ee11d91-f7e8-481d-b079-65d708582ccc";
  private static final String PRE_POPULATED_TYPE3_ID = "615b8413-82d5-4203-aa6e-e37984cb5ac3";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ContributionRepository repository;
  @Autowired
  private ContributionMapper mapper;
  @MockBean
  private InventoryClient inventoryClient;
  @MockBean
  private InnReachLocationExternalService irLocationService;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
    "classpath:db/contribution/pre-populate-contribution.sql"
  })
  void shouldGetCurrentContribution() {
    when(inventoryClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity =
      testRestTemplate.getForEntity(currentContributionUrl(), ContributionDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var existing = fetchCurrentContribution();

    assertNotNull(existing);

    assertEquals(MappingValidationStatusDTO.VALID, response.getItemTypeMappingStatus());
    assertEquals(MappingValidationStatusDTO.VALID, response.getLocationsMappingStatus());

    assertThat(existing, samePropertyValuesAs(response, "itemTypeMappingStatus", "locationsMappingStatus"));
  }

  private List<InnReachLocationDTO> createIrLocations() {
    return Arrays.asList("q1w2e", "p0o9i", "u7y6t").stream()
      .map(c -> new InnReachLocationDTO(c, null))
      .collect(Collectors.toList());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution/pre-populate-contribution.sql"
  })
  void shouldGetContributionHistory() {
    when(inventoryClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());

    var responseEntity =
      testRestTemplate.getForEntity(contributionHistoryUrl(), ContributionsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var existing = fetchContributionHistory();

    assertThat(existing, samePropertyValuesAs(response, "itemTypeMappingStatus", "locationsMappingStatus"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void shouldReturnNotStartedContribution() {
    when(inventoryClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity =
      testRestTemplate.getForEntity(currentContributionUrl(), ContributionDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    assertEquals(ContributionDTO.StatusEnum.NOT_STARTED, response.getStatus());
    assertEquals(MappingValidationStatusDTO.VALID, response.getItemTypeMappingStatus());
    assertEquals(MappingValidationStatusDTO.VALID, response.getLocationsMappingStatus());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void shouldReturnInvalidStatusOnException() {
    when(inventoryClient.getMaterialTypes(anyString(), anyInt())).thenThrow(new RuntimeException("test"));
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity =
      testRestTemplate.getForEntity(currentContributionUrl(), ContributionDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    assertEquals(ContributionDTO.StatusEnum.NOT_STARTED, response.getStatus());
    assertEquals(MappingValidationStatusDTO.INVALID, response.getItemTypeMappingStatus());
    assertEquals(MappingValidationStatusDTO.VALID, response.getLocationsMappingStatus());
  }

  private ResultList<MaterialTypeDTO> createMaterialTypes() {
    List<MaterialTypeDTO> results = Arrays.asList(PRE_POPULATED_TYPE_ID, PRE_POPULATED_TYPE2_ID, PRE_POPULATED_TYPE3_ID)
      .stream()
      .map(this::createMaterialType)
      .collect(Collectors.toList());

    return ResultList.of(results.size(), results);
  }

  private MaterialTypeDTO createMaterialType(String id) {
    MaterialTypeDTO dto = new MaterialTypeDTO();
    dto.setId(UUID.fromString(id));
    return dto;
  }

  private ContributionDTO fetchCurrentContribution() {
    return repository.fetchCurrentByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID)
      .map(mapper::toDTO)
      .orElse(null);
  }

  private ContributionsDTO fetchContributionHistory() {
    return mapper.toDTOCollection(repository.fetchHistoryByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID, PageRequest.of(0, 10)));
  }

  private static String currentContributionUrl() {
    return baseMappingUrl(PRE_POPULATED_CENTRAL_SERVER_ID.toString()) + "/current";
  }

  private static String contributionHistoryUrl() {
    return baseMappingUrl(PRE_POPULATED_CENTRAL_SERVER_ID.toString()) + "/history";
  }

  private static String baseMappingUrl(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/contributions";
  }

}
