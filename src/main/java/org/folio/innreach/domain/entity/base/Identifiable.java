package org.folio.innreach.domain.entity.base;

import org.springframework.lang.Nullable;

public interface Identifiable<ID> {

  @Nullable
  ID getId();

  void setId(@Nullable ID id);

}
