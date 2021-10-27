package org.folio.innreach.util;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

import org.folio.innreach.domain.dto.folio.ResultList;

@UtilityClass
public class ListUtils {

  public static <T> Stream<T> toStream(Collection<T> source) {
    return emptyIfNull(source).stream();
  }

  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return toStream(source).map(mapper).collect(toList());
  }

  public static <T, R> List<R> mapItemsWithFilter(Collection<T> source, Function<? super T, ? extends R> mapper,
                                                  Predicate<? super R> predicate) {
    return toStream(source).map(mapper).filter(predicate).collect(toList());
  }

  public static <T, R> List<R> flatMapItems(Collection<T> source, Function<? super T, ? extends Stream<? extends R>> mapper) {
    return toStream(source).flatMap(mapper).collect(toList());
  }

  public static <T, R> Optional<R> mapFirstItem(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return toStream(source).findFirst().map(mapper);
  }

  public static <T, R> Optional<R> mapFirstItem(ResultList<T> source, Function<? super T, ? extends R> mapper) {
    return mapFirstItem(source.getResult(), mapper);
  }

}
