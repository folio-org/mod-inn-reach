package org.folio.innreach.repository;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
@Sql(scripts = "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql")
class MaterialTypeMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_MAPPING1_ID = "71bd0beb-28cb-40bb-9f40-87463d61a553";
  private static final String PRE_POPULATED_MAPPING2_ID = "d9985d0d-b121-4ccd-ac16-5ebd0ccccf7f";
  private static final String PRE_POPULATED_MAPPING3_ID = "57fad69e-8c91-48c0-a61f-a6122f52737a";

  @Autowired
  private MaterialTypeMappingRepository repository;

  @Test
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(3, mappings.size());

    List<String> ids = mappings.stream()
        .map(mapping -> mapping.getId().toString())
        .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_MAPPING1_ID, PRE_POPULATED_MAPPING2_ID, PRE_POPULATED_MAPPING3_ID));
  }

}
