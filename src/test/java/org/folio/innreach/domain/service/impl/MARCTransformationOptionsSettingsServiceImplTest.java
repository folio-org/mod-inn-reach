package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.mapper.DateMapper;
import org.folio.innreach.mapper.MARCTransformationOptionsSettingsMapper;
import org.folio.innreach.mapper.MARCTransformationOptionsSettingsMapperImpl;
import org.folio.innreach.repository.MARCTransformationOptionsSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;
import java.util.UUID;

import static org.folio.innreach.fixture.MARCTransformationOptionsSettingsFixture.createMARCTransformOptSet;
import static org.folio.innreach.fixture.MARCTransformationOptionsSettingsFixture.createMARCTransformOptSetDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MARCTransformationOptionsSettingsServiceImplTest {
  @Mock
  private MARCTransformationOptionsSettingsRepository repository;

  @Spy
  private final MARCTransformationOptionsSettingsMapper mapper = new MARCTransformationOptionsSettingsMapperImpl(new DateMapper());

  @InjectMocks
  private MARCTransformationOptionsSettingsServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getMARCTransformOptSet_when_marcTransformOptSetExists() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.of(createMARCTransformOptSet()));

    var marcTransformOptSet = service.get(UUID.randomUUID());

    verify(repository).findOneByCentralServerId(any());

    assertNotNull(marcTransformOptSet);
  }

  @Test
  void createMARCTransformOptSet_when_centralServerIsNew() {
    var marcTransformOptSetDTO = createMARCTransformOptSetDTO();

    when(repository.save(any(MARCTransformationOptionsSettings.class))).thenReturn(new MARCTransformationOptionsSettings());

    var created = service.create(UUID.randomUUID(), marcTransformOptSetDTO);

    verify(mapper).toEntity(any(MARCTransformationOptionsSettingsDTO.class));
    verify(repository).save(any(MARCTransformationOptionsSettings.class));

    assertNotNull(created);
  }

  @Test
  void updateMARCTransformOptSet_when_marcTransformOptSetExists() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.of(createMARCTransformOptSet()));

    var updated = createMARCTransformOptSetDTO();

    var updatedDTO = service.update(UUID.randomUUID(), updated);

    verify(repository).findOneByCentralServerId(any());

    assertNotNull(updatedDTO);
    assertEquals(updated.getModifiedFieldsForContributedRecords().size(), updatedDTO.getModifiedFieldsForContributedRecords().size());
    assertEquals(updated.getConfigIsActive(), updatedDTO.getConfigIsActive());
    assertEquals(updated.getExcludedMARCFields(), updatedDTO.getExcludedMARCFields());
  }

  @Test
  void throwException_when_marcTransformOptSetDoesNotExist() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.empty());

    UUID id = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () -> service.get(id));

    verify(repository).findOneByCentralServerId(any());
  }
}
