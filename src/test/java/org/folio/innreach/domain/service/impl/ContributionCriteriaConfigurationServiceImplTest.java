package org.folio.innreach.domain.service.impl;

import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.mapper.ContributionCriteriaConfigurationMapper;
import org.folio.innreach.repository.ContributionCriteriaConfigurationRepository;

class ContributionCriteriaConfigurationServiceImplTest {

  private static ContributionCriteriaConfiguration TEST_DEFINED_CRITERIA_CONFIGURATION;

  @Mock
  ContributionCriteriaConfigurationRepository criteriaConfigurationRepository;

  @Spy
    private final ContributionCriteriaConfigurationMapper criteriaConfigurationMapper
    = Mappers.getMapper(ContributionCriteriaConfigurationMapper.class);

  @InjectMocks
  ContributionCriteriaConfigurationServiceImpl criteriaConfigurationService;

  /*@BeforeAll
  static void init() {
    TEST_DEFINED_CRITERIA_CONFIGURATION =
      ContributionCriteriaConfigurationFixture
        .createTestContributionCriteriaConfiguration(ContributionCriteriaConfigurationFixture.CENTRAL_SERVER_UUID);
  }*/

  /*@Test
  void contributionCriteriaConfigurationMapperTest() {
    var criteriaConfigurationDTO
      = criteriaConfigurationMapper.toDto(TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size(),
      criteriaConfigurationDTO.getStatisticalCodeBehaviors().size());
    assertEquals(TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size(),criteriaConfigurationDTO
      .getExcludedLocations().size());
    var criteriaConfigurationMapped
      = criteriaConfigurationMapper.toEntity(criteriaConfigurationDTO);
    assertEquals(TEST_DEFINED_CRITERIA_CONFIGURATION.getCentralServerId(),
      criteriaConfigurationMapped.getCentralServerId());
    assertEquals(TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size(),
      criteriaConfigurationMapped.getExcludedLocations().size());
    assertEquals(TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size(),
      criteriaConfigurationMapped.getStatisticalCodeBehaviors().size());
  }

  @Test
  void create_ContributionCriteriaConfiguration_Test() {
    MockitoAnnotations.initMocks(this);
    var contributionCriteriaConfigurationDTO
      = criteriaConfigurationMapper.toDto(TEST_DEFINED_CRITERIA_CONFIGURATION);
    var criteriaConfiguration
      = contributionCriteriaMapper.toContributionCriteriaDTO(contributionCriteriaConfigurationDTO);
    when(criteriaConfigurationRepository.save(any(ContributionCriteriaConfiguration.class)))
      .thenReturn(new ContributionCriteriaConfiguration());
    var createdContributionCriteriaConfigurationDTO
      = criteriaConfigurationService.createCriteria(criteriaConfiguration);

    verify(criteriaConfigurationMapper).toEntity(any(ContributionCriteriaConfigurationDTO.class));
    verify(criteriaConfigurationRepository).save(any());
  }

  @Test
  void get_ContributionCriteriaConfiguration_Test() {
    MockitoAnnotations.initMocks(this);
    ContributionCriteriaConfigurationDTO contributionCriteriaConfigurationDTO
      = new ContributionCriteriaConfigurationDTO();
    when(criteriaConfigurationRepository.findById(TEST_DEFINED_CRITERIA_CONFIGURATION.getCentralServerId()))
      .thenReturn(Optional.of(TEST_DEFINED_CRITERIA_CONFIGURATION));

    var mockedCriteriaConfiguration
      = criteriaConfigurationService.getCriteria(TEST_DEFINED_CRITERIA_CONFIGURATION.getCentralServerId());

    verify(criteriaConfigurationRepository).findById(any());
    verify(criteriaConfigurationMapper).toDto(TEST_DEFINED_CRITERIA_CONFIGURATION);
  }

  @Test
  void delete_ContributionCriteriaConfiguration_Test() {
    MockitoAnnotations.initMocks(this);
    when(criteriaConfigurationRepository.findById(any())).thenReturn(Optional.of(TEST_DEFINED_CRITERIA_CONFIGURATION));

    criteriaConfigurationService.deleteCriteria(UUID.randomUUID());

    verify(criteriaConfigurationRepository).findById(any());
    verify(criteriaConfigurationRepository).delete(any());
  }

  @Test
  void update_excludedLocations() {
    MockitoAnnotations.initMocks(this);
    Set<ContributionCriteriaExcludedLocationDTO> TEST_DEFINED_EXCLUDED_LOCATIONS_FOR_UPDATE
      = new HashSet<>();
    criteriaConfigurationService.updateExcludedLocations(TEST_DEFINED_EXCLUDED_LOCATIONS_FOR_UPDATE,
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(0,TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size());

    int QUANTITY_OF_ADDED_LOCATIONS=4;
    for (int i = 0; i <QUANTITY_OF_ADDED_LOCATIONS ; i++) {
      ContributionCriteriaExcludedLocationDTO excludedLocationDTO = new ContributionCriteriaExcludedLocationDTO();
      excludedLocationDTO.setExcludedLocationId(UUID.randomUUID());
      TEST_DEFINED_EXCLUDED_LOCATIONS_FOR_UPDATE.add(excludedLocationDTO);
    }
    criteriaConfigurationService.updateExcludedLocations(TEST_DEFINED_EXCLUDED_LOCATIONS_FOR_UPDATE,
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_LOCATIONS,TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size());

    List<ContributionCriteriaExcludedLocationDTO> nextIteration
      = new ArrayList<>(TEST_DEFINED_EXCLUDED_LOCATIONS_FOR_UPDATE);
    nextIteration.remove(0);
    nextIteration.remove(0);
    criteriaConfigurationService.updateExcludedLocations(new HashSet<>(nextIteration),
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_LOCATIONS-2,
      TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size());

    int QUANTITY_OF_RANDOM_ADDED_LOCATIONS = 7;
    for (int i = 0; i < QUANTITY_OF_RANDOM_ADDED_LOCATIONS; i++) {
      var excludedLocationDTOforAdd = new ContributionCriteriaExcludedLocationDTO();
      excludedLocationDTOforAdd.setExcludedLocationId(UUID.randomUUID());
      nextIteration.add(excludedLocationDTOforAdd);
    }
    criteriaConfigurationService.updateExcludedLocations(new HashSet<>(nextIteration),
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_LOCATIONS-2+QUANTITY_OF_RANDOM_ADDED_LOCATIONS,
      TEST_DEFINED_CRITERIA_CONFIGURATION.getExcludedLocations().size());
  }

  @Test
  void update_statisticalCodeBehaviors_test() {
    MockitoAnnotations.initMocks(this);
    var TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE = new HashSet<ContributionCriteriaStatisticalCodeBehaviorDTO>();
    TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().forEach(statisticalCodeBehavior -> {
      ContributionCriteriaStatisticalCodeBehaviorDTO updatedStatisticalCodeBehaviorDTO
        = new ContributionCriteriaStatisticalCodeBehaviorDTO();
      updatedStatisticalCodeBehaviorDTO.setStatisticalCodeId(statisticalCodeBehavior.getStatisticalCodeId());
      if (statisticalCodeBehavior.getContributionBehavior().ordinal()>=ContributionBehavior.values().length-1) {
        updatedStatisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.values()[0]);
      } else {
        updatedStatisticalCodeBehaviorDTO.setContributionBehavior(
          ContributionBehavior.values()[statisticalCodeBehavior.getContributionBehavior().ordinal()+1]);
      }
      TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE.add(updatedStatisticalCodeBehaviorDTO);
    });

    criteriaConfigurationService.updateStatisticalCodeBehaviors(TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE,
      TEST_DEFINED_CRITERIA_CONFIGURATION);

    assertEquals(TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE.size(),
      TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size());

    assertEquals(0, TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE.stream()
      .filter(statisticalCodeBehaviorDTO -> TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors()
        .stream().filter(statisticalCodeBehavior ->
        !(statisticalCodeBehaviorDTO.getStatisticalCodeId().equals(statisticalCodeBehavior.getStatisticalCodeId())
          && statisticalCodeBehaviorDTO.getContributionBehavior()
          .equals(statisticalCodeBehavior.getContributionBehavior()))
      ).findAny().isEmpty()
    ).collect(Collectors.toSet()).size());

    TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE.clear();
    criteriaConfigurationService.updateStatisticalCodeBehaviors(TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE,
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(0, TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size());

    int QUANTITY_OF_ADDED_STATISTICAL_CODE_BEHAVIORS = 4;
    for (int i = 0; i < QUANTITY_OF_ADDED_STATISTICAL_CODE_BEHAVIORS; i++) {
      ContributionCriteriaStatisticalCodeBehaviorDTO statisticalCodeBehaviorDTO
        = new ContributionCriteriaStatisticalCodeBehaviorDTO();
      statisticalCodeBehaviorDTO.setStatisticalCodeId(UUID.randomUUID());
      statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeButSuppress);
      TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE.add(statisticalCodeBehaviorDTO);
    }
    criteriaConfigurationService.updateStatisticalCodeBehaviors(TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE,
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_STATISTICAL_CODE_BEHAVIORS,
      TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size());

    List<ContributionCriteriaStatisticalCodeBehaviorDTO> nextIteration
      = new ArrayList<>(TEST_VAR_STATISTICAL_CODE_BEHAVIORS_FOR_UPDATE);
    nextIteration.remove(0);
    nextIteration.remove(0);
    criteriaConfigurationService.updateStatisticalCodeBehaviors(new HashSet<>(nextIteration),
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_STATISTICAL_CODE_BEHAVIORS - 2,
      TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size());

    int QUANTITY_OF_RANDOM_ADDED_STATISTICAL_CODE_BEHAVIORS = 7;
    for (int i = 0; i < QUANTITY_OF_RANDOM_ADDED_STATISTICAL_CODE_BEHAVIORS; i++) {
      var statisticalCodeBehaviorDTOforAdd = new ContributionCriteriaStatisticalCodeBehaviorDTO();
      statisticalCodeBehaviorDTOforAdd.setContributionBehavior(ContributionBehavior.contributeButSuppress);
      statisticalCodeBehaviorDTOforAdd.setStatisticalCodeId(UUID.randomUUID());
      nextIteration.add(statisticalCodeBehaviorDTOforAdd);
    }
    criteriaConfigurationService.updateStatisticalCodeBehaviors(new HashSet<>(nextIteration),
      TEST_DEFINED_CRITERIA_CONFIGURATION);
    assertEquals(QUANTITY_OF_ADDED_STATISTICAL_CODE_BEHAVIORS
        - 2 + QUANTITY_OF_RANDOM_ADDED_STATISTICAL_CODE_BEHAVIORS,
      TEST_DEFINED_CRITERIA_CONFIGURATION.getStatisticalCodeBehaviors().size());
  }*/
}
