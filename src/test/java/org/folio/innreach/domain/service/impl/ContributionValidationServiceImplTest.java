package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionCriteria;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.LocalAgencyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class ContributionValidationServiceImplTest {

  private static final ContributionCriteriaDTO CRITERIA = createContributionCriteria();
  private static final UUID DO_NOT_CONTRIBUTE_CODE_ID = CRITERIA.getDoNotContributeId();
  private static final String ELIGIBLE_SOURCE = "MARC";
  private static final String INELIGIBLE_SOURCE = "FOLIO";
  private static final UUID TEST_UUID = UUID.fromString("6802c458-b19e-476f-9187-e8ab7417ecd4");

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

    when(circulationClient.queryRequestsByItemId(any())).thenReturn(ResultList.of(0, Collections.emptyList()));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), item);

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsAlreadyRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.IN_TRANSIT));

    when(circulationClient.queryRequestsByItemId(any())).thenReturn(ResultList.of(1, List.of(new RequestDTO())));

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

  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsUnavailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var item = createItem();
    item.setStatus(new ItemStatus().name(ItemStatus.NameEnum.UNAVAILABLE));

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

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(UUID.randomUUID()));

    assertNull(suppress);
  }

  @Test
  void returnSuppressionStatus_y() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeButSuppressId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('y', suppress);
  }

  @Test
  void returnSuppressionStatus_n() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setDoNotContributeId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('n', suppress);
  }

  @Test
  void returnSuppressionStatus_g() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeAsSystemOwnedId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('g', suppress);
  }

  @Test
  void throwWhenMultipleStatisticalCodes() {
    when(contributionConfigService.getCriteria(any())).thenReturn(new ContributionCriteriaDTO());

    assertThatThrownBy(() -> service.getSuppressionStatus(UUID.randomUUID(), Collections.nCopies(3, UUID.randomUUID())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Multiple statistical codes defined");
  }

  @Test
  void testEligibleInstance_noStatisticalCodes() {
    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(TEST_UUID, TEST_UUID));
    when(centralServerService.getCentralServer(any()))
      .thenReturn(new CentralServerDTO().id(UUID.randomUUID()).localAgencies(List.of(
        new LocalAgencyDTO().id(UUID.randomUUID()).folioLibraryIds(List.of(TEST_UUID)))));

    var instance = new Instance();
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().effectiveLocationId(TEST_UUID)));

    var result = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertTrue(result);
  }

  @Test
  void testEligibleInstance_statisticalCodeAllowed() {
    var allowedStatisticalCodeId = UUID.randomUUID();
    var statisticalCodes = List.of(allowedStatisticalCodeId);

    var instance = new Instance();
    instance.setStatisticalCodeIds(statisticalCodes);
    instance.setSource(ELIGIBLE_SOURCE);
    instance.setItems(List.of(new Item().statisticalCodeIds(statisticalCodes).effectiveLocationId(TEST_UUID)));

    when(contributionConfigService.getCriteria(any())).thenReturn(CRITERIA);
    when(holdingsService.find(any())).thenReturn(Optional.empty());
    when(folioLocationService.getLocationLibraryMappings()).thenReturn(Map.of(TEST_UUID, TEST_UUID));
    when(centralServerService.getCentralServer(any()))
      .thenReturn(new CentralServerDTO().id(UUID.randomUUID()).localAgencies(List.of(
        new LocalAgencyDTO().id(UUID.randomUUID()).folioLibraryIds(List.of(TEST_UUID)))));

    var isEligible = service.isEligibleForContribution(UUID.randomUUID(), instance);

    assertTrue(isEligible);
  }

  @Test
  void testIneligibleInstance_statisticalCodeExcluded() {
    var statisticalCodes = List.of(DO_NOT_CONTRIBUTE_CODE_ID);

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
    instance.setItems(List.of(new Item().statisticalCodeIds(List.of(DO_NOT_CONTRIBUTE_CODE_ID))));

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

}
