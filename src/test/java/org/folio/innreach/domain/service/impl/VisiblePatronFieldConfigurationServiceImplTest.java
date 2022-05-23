package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.VisiblePatronFieldConfigurationFixture.createVisiblePatronFieldConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;
import org.folio.innreach.dto.VisiblePatronFieldEnum;
import org.folio.innreach.mapper.MappingMethods;
import org.folio.innreach.mapper.VisiblePatronFieldConfigurationMapper;
import org.folio.innreach.mapper.VisiblePatronFieldConfigurationMapperImpl;
import org.folio.innreach.repository.VisiblePatronFieldConfigurationRepository;

class VisiblePatronFieldConfigurationServiceImplTest {
  @Mock
  private VisiblePatronFieldConfigurationRepository repository;

  @Spy
  private final VisiblePatronFieldConfigurationMapper mapper = new VisiblePatronFieldConfigurationMapperImpl(new MappingMethods());

  @InjectMocks
  private VisiblePatronFieldConfigurationServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getVisiblePatronFieldConfig_when_visiblePatronFieldConfigExists() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.of(createVisiblePatronFieldConfiguration()));

    var config = service.get(UUID.randomUUID());

    verify(repository).findOneByCentralServerId(any());

    assertNotNull(config);
  }

  @Test
  void createVisiblePatronFieldConfig_when_visiblePatronFieldConfigIsNew() {
    var config = createVisiblePatronFieldConfiguration();
    var configDTO = mapper.toDTO(config);

    when(repository.save(any(VisiblePatronFieldConfiguration.class))).thenReturn(config);

    var created = service.create(UUID.randomUUID(), configDTO);

    verify(mapper).toEntity(any(VisiblePatronFieldConfigurationDTO.class));
    verify(repository).save(any(VisiblePatronFieldConfiguration.class));

    assertNotNull(created);
  }

  @Test
  void updateVisiblePatronFieldConfig_when_visiblePatronFieldConfigExists() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.of(new VisiblePatronFieldConfiguration()));

    var updated = mapper.toDTO(createVisiblePatronFieldConfiguration());
    updated.getFields().addAll(List.of(VisiblePatronFieldEnum.BARCODE, VisiblePatronFieldEnum.USER_CUSTOM_FIELDS));
    updated.getUserCustomFields().add("field1");

    var updatedDTO = service.update(UUID.randomUUID(), updated);

    verify(repository).findOneByCentralServerId(any());
    verify(repository).save(any());

    assertNotNull(updatedDTO);
    assertTrue(updated.getFields().containsAll(updatedDTO.getFields()));
    assertEquals(updated.getUserCustomFields(), updatedDTO.getUserCustomFields());
  }

  @Test
  void deleteVisiblePatronFieldConfig_when_visiblePatronFieldConfigExistsAndDtoIsEmpty() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.of(new VisiblePatronFieldConfiguration()));

    var updated = new VisiblePatronFieldConfigurationDTO();
    updated.setFields(Collections.emptyList());
    updated.setUserCustomFields(Collections.emptyList());

    var updatedDTO = service.update(UUID.randomUUID(), updated);

    verify(repository).findOneByCentralServerId(any());
    verify(repository).delete(any());

    assertNull(updatedDTO);
  }

  @Test
  void throwException_when_visiblePatronFieldConfigDoesNotExist() {
    when(repository.findOneByCentralServerId(any())).thenReturn(Optional.empty());

    UUID id = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () -> service.get(id));

    verify(repository).findOneByCentralServerId(any());
  }
}
