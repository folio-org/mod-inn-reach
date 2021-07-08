package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;

@Sql(
  scripts = "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachLocationControllerTest extends BaseControllerTest {

	private static final String PRE_POPULATED_LOCATION1_ID = "a1c1472f-67ec-4938-b5a8-f119e51ab79b";
	private static final String PRE_POPULATED_USER = "admin";

  @Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void return200HttpCode_and_createdInnReachLocation_when_createInnReachLocation() {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/create-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    var responseEntity = testRestTemplate.postForEntity("/inn-reach/locations", innReachLocationDTO,
        InnReachLocationDTO.class);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
  }

	@Test
	void return400HttpCode_when_createInnReachLocation_and_requestDataIsInvalid() {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/create-inn-reach-location-request.json",
        InnReachLocationDTO.class);
    innReachLocationDTO.setCode("qwerty123");

    var responseEntity = testRestTemplate.postForEntity("/inn-reach/locations", innReachLocationDTO,
        InnReachLocationDTO.class);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpStatus_and_innReachLocation_when_innReachLocationExists() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/locations/{locationId}", InnReachLocationDTO.class,
        PRE_POPULATED_LOCATION1_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertNotNull(responseEntity.getBody());

    var innReachLocationDTO = responseEntity.getBody();

    assertEquals(UUID.fromString(PRE_POPULATED_LOCATION1_ID), innReachLocationDTO.getId());

    var metadata = innReachLocationDTO.getMetadata();

    assertNotNull(metadata);
    assertEquals(PRE_POPULATED_USER, metadata.getCreatedByUsername());
    assertEquals(PRE_POPULATED_USER, metadata.getUpdatedByUsername());
  }

	@Test
	void return404HttpCode_when_innReachLocationDoesNotExist() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/locations/{locationId}", InnReachLocationDTO.class,
        UUID.randomUUID().toString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

	@Test
	@Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpCode_and_allInReachLocations_when_innReachLocationsExist() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/locations", InnReachLocationsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var innReachLocationsDTO = responseEntity.getBody();

    assertNotNull(innReachLocationsDTO);
    assertNotNull(innReachLocationsDTO.getLocations());
    assertFalse(innReachLocationsDTO.getLocations().isEmpty());
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpCode_and_updatedInnReachLocation_when_innReachLocationsExist() {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/update-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    var responseEntity = testRestTemplate.exchange("/inn-reach/locations/{locationId}", HttpMethod.PUT,
        new HttpEntity<>(innReachLocationDTO), InnReachLocationDTO.class, PRE_POPULATED_LOCATION1_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
	}

	@Test
	void return404HttpCode_when_updatableInnReachLocationDoesNotExist() {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/update-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    var responseEntity = testRestTemplate.exchange("/inn-reach/locations/{locationId}", HttpMethod.PUT,
        new HttpEntity<>(innReachLocationDTO), InnReachLocationDTO.class, UUID.randomUUID().toString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

	@Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return204HttpCode_when_deleteInnReachLocation() {
    var responseEntity = testRestTemplate.exchange("/inn-reach/locations/{locationId}", HttpMethod.DELETE,
        HttpEntity.EMPTY, InnReachLocationDTO.class, PRE_POPULATED_LOCATION1_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

	@Test
	void return404HttpCode_when_deletableInnReachLocationDoesNotExist() {
    var responseEntity = testRestTemplate.exchange("/inn-reach/locations/{locationId}", HttpMethod.DELETE,
        HttpEntity.EMPTY, InnReachLocationDTO.class, UUID.randomUUID().toString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

}
