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

import static org.folio.innreach.fixture.ContributionFixture.createIrLocations;
import static org.folio.innreach.fixture.ContributionFixture.createIterationJobResponse;
import static org.folio.innreach.fixture.ContributionFixture.createMaterialTypes;
import static org.folio.innreach.fixture.JobResponseFixture.createJobResponse;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.spring.data.OffsetRequest;

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

  @MockBean
  private InstanceStorageClient client;

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ContributionRepository repository;
  @Autowired
  private ContributionMapper mapper;
  @MockBean
  private MaterialTypesClient materialTypesClient;
  @MockBean
  private InnReachLocationExternalService irLocationService;
  @MockBean
  private ContributionJobRunner jobRunner;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
    "classpath:db/contribution/pre-populate-contribution.sql"
  })
  void shouldGetCurrentContribution() {
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
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

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution/pre-populate-contribution.sql"
  })
  void shouldGetContributionHistory() {
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());

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
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
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
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void shouldReturnInvalidStatusOnMissingTypeMappings() {
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
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

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
  })
  void shouldReturnInvalidStatusOnMissingLibraryMappings() {
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity =
      testRestTemplate.getForEntity(currentContributionUrl(), ContributionDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    assertEquals(ContributionDTO.StatusEnum.NOT_STARTED, response.getStatus());
    assertEquals(MappingValidationStatusDTO.VALID, response.getItemTypeMappingStatus());
    assertEquals(MappingValidationStatusDTO.INVALID, response.getLocationsMappingStatus());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void shouldReturnInvalidStatusOnException() {
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenThrow(new RuntimeException("test"));
    when(irLocationService.getAllLocations(any())).thenThrow(new RuntimeException("test"));

    var responseEntity =
      testRestTemplate.getForEntity(currentContributionUrl(), ContributionDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    assertEquals(ContributionDTO.StatusEnum.NOT_STARTED, response.getStatus());
    assertEquals(MappingValidationStatusDTO.INVALID, response.getItemTypeMappingStatus());
    assertEquals(MappingValidationStatusDTO.INVALID, response.getLocationsMappingStatus());
  }

  private ContributionDTO fetchCurrentContribution() {
    return repository.fetchCurrentByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID)
      .map(mapper::toDTO)
      .orElse(null);
  }

  private ContributionsDTO fetchContributionHistory() {
    return mapper.toDTOCollection(repository.fetchHistoryByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID, new OffsetRequest(0, 10)));
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

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void return201HttpCode_whenInstanceIterationStarted() {
    var jobResponse = createJobResponse();
    when(client.startInitialContribution(any(InstanceIterationRequest.class))).thenReturn(jobResponse);

    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/contributions", HttpEntity.EMPTY, Void.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

    var fromDb = repository.fetchCurrentByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID);
    assertNotNull(fromDb);
    assertEquals(Contribution.Status.IN_PROGRESS, fromDb.get().getStatus());
    assertEquals(jobResponse.getId(), fromDb.get().getJobId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution/pre-populate-contribution.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
  })
  void return409HttpCode_whenStartingInstanceIterationForNonExistingCentralServer() {
    when(client.startInitialContribution(any(InstanceIterationRequest.class))).thenReturn(createIterationJobResponse());
    when(materialTypesClient.getMaterialTypes(anyString(), anyInt())).thenReturn(createMaterialTypes());
    when(irLocationService.getAllLocations(any())).thenReturn(createIrLocations());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/contributions", HttpEntity.EMPTY, Void.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  void shouldDeserializeJsonJobResponse(){
    var jobResponse = deserializeFromJsonFile("/contribution/job-response.json", JobResponse.class);

    assertEquals("813de9bd-d1ad-4687-9fd7-3239385e5fe5", jobResponse.getId().toString());
    assertEquals(100, jobResponse.getNumberOfRecordsPublished());
    assertEquals(JobResponse.JobStatus.COMPLETED, jobResponse.getStatus());
    assertEquals(OffsetDateTime.parse("2021-06-22T09:33:35.279+00:00"), jobResponse.getSubmittedDate());
  }
}
