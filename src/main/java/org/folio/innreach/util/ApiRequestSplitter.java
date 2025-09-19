package org.folio.innreach.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.folio.innreach.domain.dto.folio.ResultList;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ApiRequestSplitter {

  /**
   * Partitions a collection into smaller lists of a specified size.
   * @param data the collection to partition
   * @param batchSize the maximum size of each partition
   * @return a list of partitions
   * @param <T> the type of elements in the collection
   */
  public static <T> List<List<T>> partition(Collection<T> data, int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Partition batchSize must be > 0");
    }

    List<T> list = new ArrayList<>(data);
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += batchSize) {
      partitions.add(new ArrayList<>(
        list.subList(i, Math.min(i + batchSize, list.size()))
      ));
    }
    return partitions;
  }

  /**
   * Executes multiple batch requests and combines their results.
   * @param requestIds set of request IDs to process
   * @param batchSize size of each batch
   * @param apiClient function that takes a list of IDs and returns a ResultList of items
   * @return combined ResultList of all items
   * @param <T> type of items in the result
   * @param <K> type of request IDs, Type of apiClient input parameter, usually the same as T's ID type, e.g., UUID or String,
   */
  public static <T, K> ResultList<T> execute(
    Set<K> requestIds, int batchSize,
    Function<List<K>, ResultList<T>> apiClient) {

    List<List<K>> partitions = ApiRequestSplitter.partition(requestIds, batchSize);

    List<T> allResults = new ArrayList<>();

    for (List<K> batch : partitions) {
      ResultList<T> batchResult = apiClient.apply(batch);
      if (batchResult.getResult() != null) {
        allResults.addAll(batchResult.getResult());
      }
    }

    ResultList<T> result = new ResultList<>();
    result.setResult(allResults);
    result.setTotalRecords(allResults.size());

    return result;
  }
}
