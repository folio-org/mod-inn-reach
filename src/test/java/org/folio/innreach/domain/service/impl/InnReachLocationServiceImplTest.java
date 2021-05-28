package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.InnReachLocationFixture.createInnReachLocation;
import static org.folio.innreach.fixture.InnReachLocationFixture.createInnReachLocationDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.mapper.DateMapper;
import org.folio.innreach.mapper.InnReachLocationMapper;
import org.folio.innreach.mapper.InnReachLocationMapperImpl;
import org.folio.innreach.repository.InnReachLocationRepository;

class InnReachLocationServiceImplTest {

  @Mock
  private InnReachLocationRepository innReachLocationRepository;

  @Spy
  private final InnReachLocationMapper innReachLocationMapper = new InnReachLocationMapperImpl(new DateMapper());

  @InjectMocks
  private InnReachLocationServiceImpl innReachLocationService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
	void createInnReachLocation_when_centralServerIsNew() {
		var innReachLocationDTO = createInnReachLocationDTO();

		when(innReachLocationRepository.save(any(InnReachLocation.class))).thenReturn(new InnReachLocation());

		var createdInnReachLocation = innReachLocationService.createInnReachLocation(innReachLocationDTO);

		verify(innReachLocationMapper).mapToInnReachLocation(any(InnReachLocationDTO.class));
		verify(innReachLocationRepository).save(any(InnReachLocation.class));

		assertNotNull(createdInnReachLocation);
	}

	@Test
	void getInnReachLocation_when_innReachLocationExists() {
    when(innReachLocationRepository.findById(any())).thenReturn(Optional.of(createInnReachLocation()));

		var innReachLocation = innReachLocationService.getInnReachLocation(UUID.randomUUID());

    verify(innReachLocationRepository).findById(any());

		assertNotNull(innReachLocation);
	}

	@Test
	void throwException_when_innReachLocationDoesNotExist() {
    when(innReachLocationRepository.findById(any())).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class,
				() -> innReachLocationService.getInnReachLocation(UUID.randomUUID()));
	}

	@Test
	void getAllInnReachLocations() {
    when(innReachLocationRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(
        createInnReachLocation(), createInnReachLocation(), createInnReachLocation())));

    var innReachLocations = innReachLocationService.getAllInnReachLocations(0, 10);

    verify(innReachLocationRepository).findAll(any(PageRequest.class));

		assertNotNull(innReachLocations);
    assertNotNull(innReachLocations.getLocations());
    assertFalse(innReachLocations.getLocations().isEmpty());
	}

	@Test
	void updateInnReachLocation_when_innReachLocationExists() {
		when(innReachLocationRepository.findById(any())).thenReturn(Optional.of(createInnReachLocation()));

		var updatedInnReachLocationData = createInnReachLocationDTO();

		var updatedReachLocationDTO = innReachLocationService.updateInnReachLocation(UUID.randomUUID(),
				updatedInnReachLocationData);

		verify(innReachLocationRepository).findById(any());

		assertNotNull(updatedReachLocationDTO);
		assertEquals(updatedInnReachLocationData.getCode(), updatedReachLocationDTO.getCode());
		assertEquals(updatedInnReachLocationData.getDescription(), updatedReachLocationDTO.getDescription());
	}

	@Test
	void throwException_when_updatableInnReachLocationDoesNotExist() {
		when(innReachLocationRepository.findById(any())).thenReturn(Optional.empty());

		var updatedInnReachLocationData = createInnReachLocationDTO();

		assertThrows(EntityNotFoundException.class,
				() -> innReachLocationService.updateInnReachLocation(UUID.randomUUID(), updatedInnReachLocationData));
	}

	@Test
	void deleteInnReachLocation_when_innReachLocationExists() {
    when(innReachLocationRepository.findById(any())).thenReturn(Optional.of(createInnReachLocation()));

    innReachLocationService.deleteInnReachLocation(UUID.randomUUID());

    verify(innReachLocationRepository).findById(any());
    verify(innReachLocationRepository).delete(any());
  }

}
