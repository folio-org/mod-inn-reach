package org.folio.innreach.domain.service.impl;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus.ON_LOAN;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.ContributionFixture.createMARCRecord;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LocalAgencyDTO;
import org.folio.innreach.dto.LocationMappingForOneLibraryDTO;
import org.folio.innreach.dto.LocationMappingsForOneLibraryDTO;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.util.DateHelper;

@ExtendWith(MockitoExtension.class)
class RecordTransformationServiceImplTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();
  private static final UUID LIBRARY_ID = UUID.fromString("5d78803e-ca04-4b4a-aeae-2c63b924518b");
  private static final UUID FOLIO_LOC_ID = UUID.randomUUID();
  private static final UUID MATERIAL_TYPE_ID = UUID.fromString("615b8413-82d5-4203-aa6e-e37984cb5ac3");
  private static final String CENTRAL_AGENCY_CODE = "fl2g2";
  private static final UUID INN_REACH_LOCATION_ID = UUID.fromString("5d78803e-ca04-4b4a-aeae-2c63b924518a");
  private static final Integer CENTRAL_ITEM_TYPE = 211;
  private static final String LOCAL_SERVER_CODE = "fli01";
  private static final Date LOAN_DUE_DATE = new Date();

  @Mock
  private MARCRecordTransformationService marcService;
  @Mock
  private ContributionValidationService validationService;

  @Mock
  private HoldingsService holdingsService;
  @Mock
  private MaterialTypeMappingService typeMappingService;
  @Mock
  private LibraryMappingService libraryMappingService;
  @Mock
  private InnReachLocationService irLocationService;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private LocationMappingService locationMappingService;
  @Mock
  private FolioLocationService folioLocationService;

  @Mock
  private CirculationClient circulationClient;

  @InjectMocks
  private RecordTransformationServiceImpl service;

  @Test
  void shouldGetBibInfo() {
    Instance instance = createInstance();

    when(marcService.transformRecord(any(UUID.class), any(Instance.class))).thenReturn(createMARCRecord());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class))).thenReturn(true);

    var bibInfo = service.getBibInfo(CENTRAL_SERVER_ID, instance);

    assertNotNull(bibInfo);
    assertEquals(instance.getHrid(), bibInfo.getBibId());
    assertEquals((Integer) instance.getItems().size(), bibInfo.getItemCount());
  }

  @Test
  void shouldGetBibInfo_noItems() {
    Instance instance = createInstance();
    instance.setItems(null);

    when(marcService.transformRecord(any(UUID.class), any(Instance.class))).thenReturn(createMARCRecord());

    var bibInfo = service.getBibInfo(CENTRAL_SERVER_ID, instance);

    assertNotNull(bibInfo);
    assertEquals(instance.getHrid(), bibInfo.getBibId());
    assertEquals(0, (int) bibInfo.getItemCount());
  }

  @Test
  void shouldGetBibInfo_excludeItem() {
    Instance instance = createInstance();

    when(marcService.transformRecord(any(UUID.class), any(Instance.class))).thenReturn(createMARCRecord());
    when(validationService.getSuppressionStatus(any(UUID.class), any())).thenReturn('n');

    var bibInfo = service.getBibInfo(CENTRAL_SERVER_ID, instance);

    assertNotNull(bibInfo);
    assertEquals(instance.getHrid(), bibInfo.getBibId());
    assertEquals(0, (int) bibInfo.getItemCount());
  }

  @Test
  void shouldGetBibItem() {
    var item = createItem().materialTypeId(MATERIAL_TYPE_ID).effectiveLocationId(FOLIO_LOC_ID);
    var irLocations = new InnReachLocationsDTO().addLocationsItem(new InnReachLocationDTO().id(INN_REACH_LOCATION_ID).code(CENTRAL_AGENCY_CODE));
    var materialTypeMappings = new MaterialTypeMappingsDTO().addMaterialTypeMappingsItem(new MaterialTypeMappingDTO().materialTypeId(MATERIAL_TYPE_ID).centralItemType(CENTRAL_ITEM_TYPE));
    var libraryMappings = new LibraryMappingsDTO().addLibraryMappingsItem(new LibraryMappingDTO().libraryId(LIBRARY_ID).innReachLocationId(INN_REACH_LOCATION_ID));
    var locationMappings = new LocationMappingsForOneLibraryDTO().addLocationMappingsItem(new LocationMappingForOneLibraryDTO().innReachLocationId(INN_REACH_LOCATION_ID).locationId(FOLIO_LOC_ID));
    var centralServer = new CentralServerDTO().id(CENTRAL_SERVER_ID).addLocalAgenciesItem(new LocalAgencyDTO().id(UUID.randomUUID()).addFolioLibraryIdsItem(LIBRARY_ID).code(LOCAL_SERVER_CODE));
    var loan = new LoanDTO().dueDate(LOAN_DUE_DATE);
    var request = new RequestDTO();
    request.setStatus(RequestStatus.OPEN_AWAITING_DELIVERY);

    when(centralServerService.getCentralServer(any())).thenReturn(centralServer);
    when(irLocationService.getAllInnReachLocations(any(), any())).thenReturn(irLocations);
    when(typeMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(materialTypeMappings);
    when(libraryMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(libraryMappings);
    when(locationMappingService.getMappingsByLibraryId(any(), any(), anyInt(), anyInt())).thenReturn(locationMappings);
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(FOLIO_LOC_ID, LIBRARY_ID));
    when(validationService.getItemCirculationStatus(any(), any())).thenReturn(ON_LOAN);
    when(circulationClient.queryRequestsByItemId(any())).thenReturn(ResultList.asSinglePage(request));
    when(circulationClient.queryLoansByItemId(any())).thenReturn(ResultList.asSinglePage(loan));

    var bibItems = service.getBibItems(CENTRAL_SERVER_ID, of(item), (i, e) -> {
      throw new RuntimeException(e);
    });

    assertNotNull(bibItems);
    assertEquals(1, bibItems.size());
    var bibItem = bibItems.get(0);

    assertNotNull(bibItem);
    assertEquals(LOCAL_SERVER_CODE, bibItem.getAgencyCode());
    assertEquals(item.getHrid(), bibItem.getItemId());
    assertEquals(ON_LOAN.getStatus(), bibItem.getItemCircStatus());
    assertEquals(CENTRAL_AGENCY_CODE, bibItem.getLocationKey());
    assertEquals(CENTRAL_ITEM_TYPE, bibItem.getCentralItemType());
    assertEquals((Integer) DateHelper.toEpochSec(LOAN_DUE_DATE), bibItem.getDueDateTime());
    assertEquals(Long.valueOf(1), bibItem.getHoldCount());
  }

}
