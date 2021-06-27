package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.experimental.UtilityClass;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.base.Identifiable;

@UtilityClass
class ServiceUtils {

  static CentralServer centralServerRef(UUID centralServerId) {
    var server = new CentralServer();
    server.setId(centralServerId);

    return server;
  }

  static <E extends Identifiable<UUID>> Consumer<E> initId() {
    return identifiable -> {
      if (identifiable.getId() == null) {
        identifiable.setId(UUID.randomUUID());
      }
    };
  }

  static <E extends Identifiable<UUID>> Comparator<E> comparatorById() {
    return Comparator.comparing(Identifiable::getId);
  }

  static <E extends Identifiable<UUID>> List<E> evaluateEntitiesToDelete(List<E> stored, List<E> incoming) {
    var sortedIncoming = new ArrayList<>(incoming);
    sortedIncoming.sort(comparatorById());

    return stored.stream()
        .filter(notPresentIn(sortedIncoming))
        .collect(toList());
  }

  private static <E extends Identifiable<UUID>> Predicate<E> notPresentIn(List<E> entities) {
    return tested -> Collections.binarySearch(entities, tested, comparatorById()) < 0;
  }
  
}
