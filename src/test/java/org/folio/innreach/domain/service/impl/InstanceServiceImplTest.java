package org.folio.innreach.domain.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;

@ExtendWith(MockitoExtension.class)
public class InstanceServiceImplTest {

  @Mock
  private InstanceServiceImpl instanceService;

  @Test
  void shouldReturnAuthorFromInstanceEntity() {
    when(instanceService.getAuthor(any(InventoryInstanceDTO.class))).thenReturn("author1");

    var contributor1 = new InventoryInstanceDTO.ContributorDTO();
    contributor1.setName("author1");
    contributor1.setPrimary(true);
    var contributor2 = new InventoryInstanceDTO.ContributorDTO();
    contributor2.setName("author2");
    contributor2.setPrimary(false);

    var contributors = new ArrayList<InventoryInstanceDTO.ContributorDTO>();
    contributors.add(contributor1);
    contributors.add(contributor2);

    var instance = new InventoryInstanceDTO();
    instance.setContributors(contributors);

    var author = instanceService.getAuthor(instance);

    assertEquals(contributors.get(0).getName(), author);

    verify(instanceService).getAuthor(any(InventoryInstanceDTO.class));
  }
}
