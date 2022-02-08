package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@ExtendWith(MockitoExtension.class)
class FolioLocationServiceImplTest {

  private static final UUID LOCATION1_ID = UUID.randomUUID();
  private static final UUID LOCATION2_ID = UUID.randomUUID();
  private static final UUID LOCATION3_ID = UUID.randomUUID();
  private static final UUID UNKNOWN_LOCATION_ID = UUID.randomUUID();

  private static final UUID LIBRARY1_ID = UUID.randomUUID();
  private static final UUID LIBRARY2_ID = UUID.randomUUID();

  @Mock
  private LocationsClient locationsClient;

  @InjectMocks
  private FolioLocationServiceImpl service;

  @Test
  void shouldReturnLocationLibraryMappings() {
    var location1 = new LocationDTO(LOCATION1_ID, LIBRARY1_ID);
    var location2 = new LocationDTO(LOCATION2_ID, LIBRARY1_ID);
    var location3 = new LocationDTO(LOCATION3_ID, LIBRARY2_ID);

    when(locationsClient.getLocations(anyInt())).thenReturn(ResultList.asSinglePage(location1, location2, location3));

    var mappings = service.getLocationLibraryMappings();

    assertEquals(LIBRARY1_ID, mappings.get(LOCATION1_ID));
    assertEquals(LIBRARY1_ID, mappings.get(LOCATION2_ID));
    assertEquals(LIBRARY2_ID, mappings.get(LOCATION3_ID));
    assertNull(mappings.get(UNKNOWN_LOCATION_ID));
  }

}
