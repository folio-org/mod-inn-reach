package org.folio.innreach.domain.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createMARCRecord;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@ExtendWith(MockitoExtension.class)
class RecordTransformationServiceImplTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();

  @Mock
  private MARCRecordTransformationService marcService;
  @Mock
  private ContributionValidationService validationService;

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

}
