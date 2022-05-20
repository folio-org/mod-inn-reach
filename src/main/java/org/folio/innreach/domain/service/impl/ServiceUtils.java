package org.folio.innreach.domain.service.impl;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.base.Identifiable;

@UtilityClass
class ServiceUtils {

  static final Sort SORT_BY_ID = Sort.by("id");
  static final Sort DEFAULT_SORT = SORT_BY_ID;


  static <T, U> BiConsumer<T, U> nothing() {
    return (t, u) -> {};
  }

  static CentralServer centralServerRef(UUID centralServerId) {
    var server = new CentralServer();
    server.setId(centralServerId);

    return server;
  }

  static <E extends Identifiable<UUID>> void initId(E identifiable) {
    if (identifiable.getId() == null) {
      identifiable.setId(UUID.randomUUID());
    }
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

  static <E extends Identifiable<UUID>> List<E> mergeAndSave(List<E> incomingEntities, List<E> storedEntities,
      JpaRepository<E, UUID> repository, BiConsumer<E, E> updateDataMethod) {

    List<E> toDelete = new ArrayList<>();
    List<E> toSave = new ArrayList<>();

    merge(incomingEntities, storedEntities, comparatorById(),
        toSave::add,
        (incoming, stored) -> {
          updateDataMethod.accept(incoming, stored);
          toSave.add(stored);
        },
        toDelete::add);

    repository.flush();

    repository.deleteAllInBatch(toDelete);

    return repository.saveAllAndFlush(toSave);
  }

  static <E extends Comparable<E>> void merge(Collection<E> incoming, Collection<E> stored,
      Consumer<E> addMethod, BiConsumer<E, E> updateMethod, Consumer<E> deleteMethod) {
    merge(incoming, stored, Comparable::compareTo, addMethod, updateMethod, deleteMethod);
  }

  static <E> void merge(Collection<E> incoming, Collection<E> stored, Comparator<E> comparator,
      Consumer<E> addMethod, BiConsumer<E, E> updateMethod, Consumer<E> deleteMethod) {

    var storedList = new ArrayList<>(emptyIfNull(stored));

    var incomingList = new ArrayList<>(emptyIfNull(incoming));
    incomingList.sort(comparator);

    storedList.forEach(s -> {
      int idx = Collections.binarySearch(incomingList, s, comparator);

      if (idx >= 0) { // updating
        updateMethod.accept(incomingList.get(idx), s);

        incomingList.remove(idx);
      } else { // removing
        deleteMethod.accept(s);
      }
    });

    incomingList.forEach(addMethod); // what is left in the incoming has to be inserted
  }

}
