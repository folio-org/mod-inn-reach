package org.folio.innreach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.junit.jupiter.api.Test;

class ApiRequestSplitterTest {

  @Test
  void testPartition_evenBatches() {
    List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6);
    List<List<Integer>> partitions = ApiRequestSplitter.partition(data, 2);
    assertEquals(3, partitions.size());
    assertEquals(Arrays.asList(1, 2), partitions.get(0));
    assertEquals(Arrays.asList(3, 4), partitions.get(1));
    assertEquals(Arrays.asList(5, 6), partitions.get(2));
  }

  @Test
  void testPartition_unevenBatches() {
    List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
    List<List<Integer>> partitions = ApiRequestSplitter.partition(data, 2);
    assertEquals(3, partitions.size());
    assertEquals(Arrays.asList(1, 2), partitions.get(0));
    assertEquals(Arrays.asList(3, 4), partitions.get(1));
    assertEquals(Collections.singletonList(5), partitions.get(2));
  }

  @Test
  void testPartition_batchSizeGreaterThanData() {
    List<Integer> data = Arrays.asList(1, 2, 3);
    List<List<Integer>> partitions = ApiRequestSplitter.partition(data, 10);
    assertEquals(1, partitions.size());
    assertEquals(Arrays.asList(1, 2, 3), partitions.get(0));
  }

  @Test
  void testPartition_emptyData() {
    List<Integer> data = Collections.emptyList();
    List<List<Integer>> partitions = ApiRequestSplitter.partition(data, 2);
    assertTrue(partitions.isEmpty());
  }

  @Test
  void testPartition_invalidBatchSize() {
    List<Integer> data = Arrays.asList(1, 2, 3);
    assertThrows(IllegalArgumentException.class, () -> ApiRequestSplitter.partition(data, 0));
    assertThrows(IllegalArgumentException.class, () -> ApiRequestSplitter.partition(data, -1));
  }

  @Test
  void testExecute_combinesResults() {
    Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
    int batchSize = 2;

    Function<List<Integer>, ResultList<String>> apiClient = batch -> {
      ResultList<String> result = new ResultList<>();
      List<String> strings = new ArrayList<>();
      for (Integer id : batch) {
        strings.add("item-" + id);
      }
      result.setResult(strings);
      result.setTotalRecords(strings.size());
      return result;
    };

    ResultList<String> result = ApiRequestSplitter.execute(ids, batchSize, apiClient);

    assertEquals(5, result.getTotalRecords());
    assertTrue(result.getResult().containsAll(Arrays.asList("item-1", "item-2", "item-3", "item-4", "item-5")));
  }

  @Test
  void testExecute_emptyInput() {
    Set<Integer> ids = Collections.emptySet();
    int batchSize = 3;

    Function<List<Integer>, ResultList<String>> apiClient = batch -> {
      ResultList<String> result = new ResultList<>();
      result.setResult(Collections.emptyList());
      result.setTotalRecords(0);
      return result;
    };

    ResultList<String> result = ApiRequestSplitter.execute(ids, batchSize, apiClient);

    assertEquals(0, result.getTotalRecords());
    assertTrue(result.getResult().isEmpty());
  }

  @Test
  void testExecute_nullResultsHandled() {
    Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2));
    int batchSize = 1;

    Function<List<Integer>, ResultList<String>> apiClient = batch -> {
      ResultList<String> result = new ResultList<>();
      result.setResult(null); // Simulate null result
      result.setTotalRecords(0);
      return result;
    };

    ResultList<String> result = ApiRequestSplitter.execute(ids, batchSize, apiClient);

    assertEquals(0, result.getTotalRecords());
    assertTrue(result.getResult().isEmpty());
  }
}