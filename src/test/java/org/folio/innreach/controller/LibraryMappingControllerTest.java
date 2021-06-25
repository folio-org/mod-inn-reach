package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.mapper.LibraryMappingMapper;
import org.folio.innreach.repository.LibraryMappingRepository;

@Sql(
    scripts = {
        "classpath:db/lib-mapping/clear-library-mapping-table.sql",
        "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
        "classpath:db/central-server/clear-central-server-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class LibraryMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private LibraryMappingRepository repository;
  @Autowired
  private LibraryMappingMapper mapper;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldGetAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), LibraryMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getLibraryMappings();

    List<LibraryMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/libraries/location-mappings";
  }

}
