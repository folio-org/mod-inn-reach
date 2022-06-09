package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.InventoryFixture.createInventoryInstance;
import static org.folio.innreach.fixture.InventoryFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.TestUtil.readFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;
import org.folio.innreach.dto.PagingSlipsDTO;
import org.folio.innreach.external.client.feign.InnReachClient;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql",
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class PagingSlipControllerTest extends BaseControllerTest {

  private static final String INN_REACH_LOCAL_SERVERS_URI = ".*/contribution/localservers";
  private static final String INN_REACH_PATRON_TYPES_URI = ".*/circ/patrontypes";

  private static final String LOCAL_SERVERS_RESPONSE_PATH = "json/d2ir/d2ir-local-servers-response.json";
  private static final String PATRON_TYPES_RESPONSE_PATH = "json/d2ir/d2ir-patron-types-response.json";

  private static final UUID PRE_POPULATED_ITEM_ID = UUID.fromString("4def31b0-2b60-4531-ad44-7eab60fa5428");
  private static final UUID PRE_POPULATED_INSTANCE_ID = UUID.fromString("891bfff3-ba79-4beb-8c25-f714f14c6a32");
  private static final String PRE_POPULATED_PATRON_NAME = "patronName2";
  private static final String PRE_POPULATED_PATRON_AGENCY_CODE = "qwe56";
  private static final String PRE_POPULATED_ITEM_AGENCY_CODE = "asd78";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 1;
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";
  private static final String PRE_POPULATED_LOCAL_SERVER_CODE = "test1";
  private static final String PRE_POPULATED_PICKUP_LOCATION_CODE = "pickupLocCode2";
  private static final String PRE_POPULATED_PICKUP_LOCATION_NAME = "displayName2";
  private static final String PRE_POPULATED_PICKUP_LOCATION_PRINT_NAME = "printName2";
  private static final String PRE_POPULATED_PICKUP_LOCATION_DELIVERY_STOP = null;
  private static final String PRE_POPULATED_CENTRAL_SERVER_NAME = "name";
  private static final String CENTRAL_PATRON_DESCRIPTION = "Patron";
  private static final String PATRON_AGENCY_DESCRIPTION = "Test agency 1";
  private static final String ITEM_AGENCY_DESCRIPTION = "Test agency 2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @MockBean
  private CirculationClient circulationClient;

  @MockBean
  private InventoryClient inventoryClient;

  @MockBean
  private LocationsClient locationsClient;

  @MockBean
  private InnReachClient innReachClient;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void returnPagingSlips() {
    var item = createInventoryItemDTO();
    item.setId(PRE_POPULATED_ITEM_ID);

    var author = new InventoryInstanceDTO.ContributorDTO();
    author.setPrimary(true);
    author.setName("John Doe");

    var instance = createInventoryInstance();
    instance.setId(PRE_POPULATED_INSTANCE_ID);
    instance.setContributors(List.of(author));

    var request = new RequestDTO();
    request.setItemId(UUID.randomUUID());

    var location = new LocationDTO();
    location.setId(item.getEffectiveLocation().getId());

    when(locationsClient.queryLocationsByServicePoint(any(), anyInt())).thenReturn(ResultList.asSinglePage(location));
    when(circulationClient.queryNotFilledRequestsByIds(any(), anyInt())).thenReturn(ResultList.asSinglePage(request));
    when(inventoryClient.queryItemsByIdsAndLocations(any(), any(), anyInt())).thenReturn(ResultList.asSinglePage(item));
    when(inventoryClient.queryInstancesByIds(any(), anyInt())).thenReturn(ResultList.asSinglePage(instance));

    when(innReachClient.callInnReachApi(matchURI(INN_REACH_LOCAL_SERVERS_URI), any(), any(), any()))
      .thenReturn(readFile(LOCAL_SERVERS_RESPONSE_PATH));
    when(innReachClient.callInnReachApi(matchURI(INN_REACH_PATRON_TYPES_URI), any(), any(), any()))
      .thenReturn(readFile(PATRON_TYPES_RESPONSE_PATH));

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/paging-slips/{servicePointId}", PagingSlipsDTO.class, UUID.randomUUID()
    );

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var pagingSlips = responseEntity.getBody();
    assertNotNull(pagingSlips);
    assertEquals(1, pagingSlips.getTotalRecords());

    var pagingSlip = pagingSlips.getPagingSlips().get(0);
    assertNotNull(pagingSlip);

    var slip = pagingSlip.getSlip();
    assertNotNull(slip);
    assertEquals("INN-Reach Paging Slip - " + PRE_POPULATED_CENTRAL_SERVER_NAME, slip.getName());

    var itemSlip = pagingSlip.getItem();
    assertNotNull(itemSlip);
    assertEquals(item.getEffectiveLocation().getName(), itemSlip.getEffectiveLocationFolioName());
    assertEquals(item.getTitle(), itemSlip.getTitle());
    assertEquals(item.getBarcode(), itemSlip.getBarcode());
    assertEquals(item.getCallNumber(), itemSlip.getEffectiveCallNumber());
    assertEquals(item.getEffectiveShelvingOrder(), itemSlip.getShelvingOrder());
    assertEquals(item.getHrid(), itemSlip.getHrid());
    assertEquals(author.getName(), itemSlip.getAuthor());

    var transactionSlip = pagingSlip.getInnReachTransaction();
    assertEquals(PRE_POPULATED_PATRON_NAME, transactionSlip.getPatronName());
    assertEquals(PRE_POPULATED_PATRON_AGENCY_CODE, transactionSlip.getPatronAgencyCode());
    assertEquals(PATRON_AGENCY_DESCRIPTION, transactionSlip.getPatronAgencyDescription());
    assertEquals(PRE_POPULATED_CENTRAL_PATRON_TYPE, transactionSlip.getPatronTypeCode());
    assertEquals(CENTRAL_PATRON_DESCRIPTION, transactionSlip.getPatronTypeDescription());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, transactionSlip.getCentralServerCode());
    assertEquals(PRE_POPULATED_LOCAL_SERVER_CODE, transactionSlip.getLocalServerCode());
    assertEquals(PRE_POPULATED_ITEM_AGENCY_CODE, transactionSlip.getItemAgencyCode());
    assertEquals(ITEM_AGENCY_DESCRIPTION, transactionSlip.getItemAgencyDescription());
    assertEquals(PRE_POPULATED_PICKUP_LOCATION_CODE, transactionSlip.getPickupLocationCode());
    assertEquals(PRE_POPULATED_PICKUP_LOCATION_NAME, transactionSlip.getPickupLocationDisplayName());
    assertEquals(PRE_POPULATED_PICKUP_LOCATION_PRINT_NAME, transactionSlip.getPickupLocationPrintName());
    assertEquals(PRE_POPULATED_PICKUP_LOCATION_DELIVERY_STOP, transactionSlip.getPickupLocationDeliveryStop());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void returnEmptySlips_whenTransactionsNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/paging-slips/{servicePointId}", PagingSlipsDTO.class, UUID.randomUUID()
    );

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var pagingSlips = responseEntity.getBody();
    assertNotNull(pagingSlips);
    assertEquals(0, pagingSlips.getTotalRecords());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void returnEmptySlips_whenLocationsNotFound() {
    when(locationsClient.queryLocationsByServicePoint(any(), anyInt())).thenReturn(ResultList.asSinglePage(new LocationDTO()));
    when(circulationClient.queryNotFilledRequestsByIds(any(), anyInt())).thenReturn(ResultList.empty());

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/paging-slips/{servicePointId}", PagingSlipsDTO.class, UUID.randomUUID()
    );

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var pagingSlips = responseEntity.getBody();
    assertNotNull(pagingSlips);
    assertEquals(0, pagingSlips.getTotalRecords());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void returnEmptySlips_whenRequestsNotFound() {
    when(locationsClient.queryLocationsByServicePoint(any(), anyInt())).thenReturn(ResultList.empty());

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/paging-slips/{servicePointId}", PagingSlipsDTO.class, UUID.randomUUID()
    );

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var pagingSlips = responseEntity.getBody();
    assertNotNull(pagingSlips);
    assertEquals(0, pagingSlips.getTotalRecords());
  }

  private static URI matchURI(String uriPathPattern) {
    return argThat(uri -> uri != null && uri.getPath().matches(uriPathPattern));
  }

}
