package org.folio.innreach.domain.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.ArrayList;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.service.InstanceService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.Assert.assertEquals;

class InstanceServiceImplTest {

  @MockitoBean
  private InventoryClient inventoryClient;
  @InjectMocks
  private InstanceService instanceService = new InstanceServiceImpl(inventoryClient);

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
}
