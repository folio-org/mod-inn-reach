package org.folio.innreach.domain.service.impl;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.repository.JpaRepository;

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

  static <E extends Identifiable<UUID>> boolean equalIds(E first, E second) {
    return nonNull(first) && nonNull(second) &&
        Objects.equals(first.getId(), second.getId());
  }

  static <E extends Identifiable<UUID>> List<E> merge(List<E> incomingEntities, List<E> storedEntities,
      JpaRepository<E, UUID> repository, BiConsumer<E, E> updateDataMethod) {

    List<E> toDelete = new ArrayList<>();
    List<E> toSave = new ArrayList<>();

    var sortedIncoming = new ArrayList<>(incomingEntities);
    sortedIncoming.sort(comparatorById());

    storedEntities.forEach(stored -> {
      int idx = Collections.binarySearch(sortedIncoming, stored, comparatorById());

      if (idx >= 0) { // updating
        updateDataMethod.accept(sortedIncoming.get(idx), stored);

        toSave.add(stored);
        sortedIncoming.remove(idx);
      } else { // removing
        toDelete.add(stored);
      }
    });
    toSave.addAll(sortedIncoming); // what is left in the incoming has to be inserted

    repository.deleteInBatch(toDelete);

    return repository.saveAll(toSave);
  }

}
