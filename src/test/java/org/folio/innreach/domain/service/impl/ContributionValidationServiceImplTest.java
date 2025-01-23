package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionCriteria;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.dto.LocalAgencyDTO;
import org.folio.innreach.fixture.CentralServerFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemStatus;
import org.folio.innreach.external.service.InnReachLocationExternalService;

@Log4j2
class ContributionValidationServiceImplTest {

  private static final ContributionCriteriaDTO CRITERIA = createContributionCriteria();
  private static final UUID DO_NOT_CONTRIBUTE_CODE_ID = CRITERIA.getDoNotContributeId();
  private static final String ELIGIBLE_SOURCE = "MARC";
  private static final String INELIGIBLE_SOURCE = "FOLIO";
  private static final UUID LOCATION_ID = UUID.fromString("6802c458-b19e-476f-9187-e8ab7417ecd4");
  private static final UUID LIBRARY_ID = UUID.fromString("97858fdf-1e48-4eff-abb3-82421c530368");
  private static final UUID ASSOCIATED_ITEM_EFFECTIVE_LOCATION_ID =
          UUID.fromString("cbd7c258-4484-f122-9426-cb043e1ad100");

  @Mock
  private MaterialTypesClient materialTypesClient;
  @Mock
  private MaterialTypeMappingService typeMappingService;
  @Mock
  private LibraryMappingService libraryMappingService;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private ContributionCriteriaConfigurationService contributionConfigService;
  @Mock
  private InnReachLocationService innReachLocationService;
  @Mock
  private InnReachLocationExternalService innReachLocationExternalService;
  @Mock
  private ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;
  @Mock
  private InventoryClient inventoryClient;
  @Mock
  private CirculationClient circulationClient;
  @Mock
  private HoldingsService holdingsService;
  @Mock
  private FolioLocationService folioLocationService;

  @InjectMocks
  private ContributionValidationServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsAvailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsNotRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.IN_TRANSIT));
    item.setPermanentLoanTypeId(UUID.fromString("2b94c631-fca9-4892-a730-03ee529ffe27"));
    item.setTemporaryLoanTypeId(UUID.fromString("2b94c631-fca9-4892-a730-03ee529ffe27"));
    item.setEffectiveLocationId(UUID.fromString("fcd64ce1-6995-48f0-840e-89ffa2288371"));
    item.setMaterialTypeId(UUID.fromString("1a54b431-2e4f-452d-9cae-9cee66c9a892"));

    when(circulationClient.queryRequestsByItemIdAndStatus(any(),anyInt())).thenReturn(ResultList.of(0, Collections.emptyList()));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsAlreadyRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.IN_TRANSIT));

    when(circulationClient.queryRequestsByItemIdAndStatus(any(),anyInt())).thenReturn(ResultList.of(1, List.of(new RequestDTO())));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsNotListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.AWAITING_PICKUP));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @ParameterizedTest
  @EnumSource(value = ItemStatus.NameEnum.class, names = { "LONG_MISSING", "DECLARED_LOST", "AGED_TO_LOST", "AWAITING_PICKUP", "PAGED", "WITHDRAWN" })
  void returnNotAvailableContributionStatusForSpecificItemStatuses(ItemStatus.NameEnum status) {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any()))
      .thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(status));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }


  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.AGED_TO_LOST));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnOnLoanContributionStatusWhenItemStatusIsCheckedOut() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.CHECKED_OUT));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.ON_LOAN, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByLoanTypes() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableLoanTypes = itmContribOptConfDTO.getNonLendableLoanTypes();

    var item = createItem();
    item.setPermanentLoanTypeId(nonLendableLoanTypes.get(0));
    item.setTemporaryLoanTypeId(nonLendableLoanTypes.get(1));

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByLocations() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableLocations = itmContribOptConfDTO.getNonLendableLocations();

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE));
    item.setEffectiveLocationId(nonLendableLocations.get(0));

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByMaterialTypes() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableMaterialTypes = itmContribOptConfDTO.getNonLendableMaterialTypes();

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE));
    item.setMaterialTypeId(nonLendableMaterialTypes.get(0));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNullSuppressionStatus() {
    when(contributionConfigService.getCriteria(any())).thenReturn(new ContributionCriteriaDTO());

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), Set.of(UUID.randomUUID()));

    assertNull(suppress);
  }

  @Test
  void returnNullSuppressionStatusWithNoCriteriaConfiguration() {
    when(contributionConfigService.getCriteria(any())).thenReturn(null);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), Set.of(UUID.randomUUID()));

    assertNull(suppress);
  }

  @Test
  void returnSuppressionStatus_y() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeButSuppressId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), Set.of(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('y', suppress);
  }

  @Test
  void returnSuppressionStatus_n() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setDoNotContributeId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), Set.of(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('n', suppress);
  }

  @Test
  void returnSuppressionStatus_g() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeAsSystemOwnedId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), Set.of(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('g', suppress);
  }

  @Test
  void throwWhenMultipleStatisticalCodes() {
    when(contributionConfigService.getCriteria(any())).thenReturn(new ContributionCriteriaDTO());

    var statisticalCodeIds = new HashSet<UUID>(3);
    for (int i = 0; i < 3; i++) {
      statisticalCodeIds.add(UUID.randomUUID());
    }

    UUID centralServerId = UUID.randomUUID();
    assertThatThrownBy(() -> service.getSuppressionStatus(centralServerId, statisticalCodeIds))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Multiple statistical codes defined");
  }

  @Test
  void testEligibleInstance_noStatisticalCodes() {
    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(LOCATION_ID, LIBRARY_ID));
    when(centralServerService.getCentralServer(any())).thenReturn(createCentralServerWithLibraryId());

    var instance = new Instance();
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().effectiveLocationId(LOCATION_ID)));

    var result = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertTrue(result);
  }

  @Test
  void testEligibleInstance_statisticalCodeAllowed() {
    var allowedStatisticalCodeId = UUID.randomUUID();
    var statisticalCodes = Set.of(allowedStatisticalCodeId);

    var instance = new Instance();
    instance.setStatisticalCodeIds(statisticalCodes);
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().statisticalCodeIds(statisticalCodes).effectiveLocationId(LOCATION_ID)));

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(LOCATION_ID, LIBRARY_ID));
    when(centralServerService.getCentralServer(any())).thenReturn(createCentralServerWithLibraryId());

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertTrue(isEligible);
  }

  @Test
  void testNotEligibleInstanceWhenItemsLocationNotAssociatedWithInnreachLibrary() {
    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(UUID.randomUUID(), UUID.randomUUID()));
    when(centralServerService.getCentralServer(any())).thenReturn(CentralServerFixture.createCentralServerDTO());

    var instance = new Instance();
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().effectiveLocationId(UUID.randomUUID())));

    var result = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(result);
  }

  @Test
  void testEligibleItemLocationAssociatedWithExcludedFolioLocationToDoNotContribute() {
    CRITERIA.getLocationIds().add(ASSOCIATED_ITEM_EFFECTIVE_LOCATION_ID);
    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(ASSOCIATED_ITEM_EFFECTIVE_LOCATION_ID, LIBRARY_ID));
    when(centralServerService.getCentralServer(any())).thenReturn(createCentralServerWithLibraryId());


    var instance = new Instance();
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().effectiveLocationId(ASSOCIATED_ITEM_EFFECTIVE_LOCATION_ID)));

    var result = service.isEligibleForContribution(UUID.randomUUID(), instance);
    assertFalse(result);
  }

  @Test
  void testEligibleItemLocationNotAssociatedWithExcludedFolioLocationToContribute() {
    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(LOCATION_ID, LIBRARY_ID));
    when(centralServerService.getCentralServer(any())).thenReturn(createCentralServerWithLibraryId());
    log.info("testEligibleItemLocationNotAssociatedWithExcludedFolioLocationToContribute");
    var instance = new Instance();
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().effectiveLocationId(LOCATION_ID)));

    var result = service.isEligibleForContribution(UUID.randomUUID(), instance);
    assertTrue(result);
  }

  @Test
  void testIneligibleInstance_statisticalCodeExcluded() {

    var statisticalCodes = Set.of(DO_NOT_CONTRIBUTE_CODE_ID, LIBRARY_ID);


    var instance = new Instance();
    instance.setStatisticalCodeIds(statisticalCodes);
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().statisticalCodeIds(statisticalCodes)));

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(isEligible);
  }

  @Test
  void testInstanceWithMoreThanOneStatisticalCodeExcluded() {

    var statisticalCodes = Set.of(DO_NOT_CONTRIBUTE_CODE_ID);


    var instance = new Instance();
    instance.setStatisticalCodeIds(statisticalCodes);
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().statisticalCodeIds(statisticalCodes)));

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(isEligible);
  }

  @Test
  void testIneligibleInstance_noEligibleItems() {
    var instance = new Instance().source(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().statisticalCodeIds(Set.of(DO_NOT_CONTRIBUTE_CODE_ID))));

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(isEligible);
  }

  @Test
  void testIneligibleInstance_noItems() {
    var instance = new Instance().source(ELIGIBLE_SOURCE);

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(isEligible);
  }

  @Test
  void testIneligibleInstance_unsupportedSource() {
    var instance = new Instance().source(INELIGIBLE_SOURCE);

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertFalse(isEligible);
  }

  private CentralServerDTO createCentralServerWithLibraryId() {
    return CentralServerFixture.createCentralServerDTO()
      .localAgencies(List.of(new LocalAgencyDTO().folioLibraryIds(List.of(LIBRARY_ID))));
  }
}
