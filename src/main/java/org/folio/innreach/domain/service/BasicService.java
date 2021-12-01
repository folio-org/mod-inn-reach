package org.folio.innreach.domain.service;

import java.util.Optional;

public interface BasicService<Key, Rec> extends UpdateTemplateWithFinder<Key, Rec> {

  Rec create(Rec rec);

  Rec update(Rec rec);

  Optional<Rec> find(Key key);

  @Override
  default Finder<Key, Rec> defaultFinder() {
    return this::find;
  }

}
