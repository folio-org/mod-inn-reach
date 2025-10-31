package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.innreach.config.props.SplitRequestIdsConfiguration;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import java.util.ArrayList;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstanceServiceImplTest {

  @Mock private InventoryClient inventoryClient;
  @Mock private SplitRequestIdsConfiguration splitIdsConfiguration;
  @InjectMocks private InstanceServiceImpl instanceService;

  @Test
  void shouldReturnAuthorFromInstanceEntity() {

    var contributor1 = new InventoryInstanceDTO.ContributorDTO();
    contributor1.setName("author1");
    contributor1.setPrimary(false);
    var contributor2 = new InventoryInstanceDTO.ContributorDTO();
    contributor2.setName("author2");
    contributor2.setPrimary(true);

    var contributors = new ArrayList<InventoryInstanceDTO.ContributorDTO>();
    contributors.add(contributor1);
    contributors.add(contributor2);

    var instance = new InventoryInstanceDTO();
    instance.setContributors(contributors);

    var author = instanceService.getAuthor(instance);

    assertEquals(contributors.get(1).getName(), author);
  }

  @Test
  void findInstancesByIds() {
    var instanceId = UUID.randomUUID();
    var expectedInstance = InventoryInstanceDTO.builder()
        .id(instanceId)
        .title("test instance")
        .build();

    var expected = ResultList.asSinglePage(expectedInstance);
    when(splitIdsConfiguration.getSplitSize()).thenReturn(50);
    when(inventoryClient.queryInstancesByIds(instanceId.toString(), 100)).thenReturn(expected);
    var actual = instanceService.findInstancesByIds(Set.of(instanceId), 100);

    assertEquals(List.of(expectedInstance), actual);
  }
}
