package org.folio.innreach.domain.service;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.Instance;

public interface PatronHoldService {

  void createVirtualItems(InnReachTransaction transaction);

  void updateVirtualItems(InnReachTransaction transaction);
}
