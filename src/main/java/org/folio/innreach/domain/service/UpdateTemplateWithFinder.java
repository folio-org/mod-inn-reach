package org.folio.innreach.domain.service;

public interface UpdateTemplateWithFinder<Key, Rec> extends UpdateTemplate<Key, Rec> {

  default Rec update(Key k, UpdateOperation<Rec> updater) {
    return update(k, defaultFinder(), updater);
  }

  Finder<Key, Rec> defaultFinder();

}
