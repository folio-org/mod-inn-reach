package org.folio.innreach.domain.dto.folio;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@JsonIgnoreProperties("resultInfo")
public class ResultList<T> {

  /**
   * Total records.
   */
  @JsonAlias("total_records")
  private int totalRecords = 0;

  /**
   * Paged result data.
   */
  private List<T> result = Collections.emptyList();

  // The `key` is required per contract
  @SuppressWarnings("unused")
  @JsonAnySetter
  public void set(String key, List<T> result) {
    this.result = result;
  }

  /**
   * Creates empty result list.
   *
   * @param <R> generic type for result item.
   * @return empty result list.
   */
  public static <R> ResultList<R> empty() {
    return new ResultList<>();
  }

  /**
   * Creates result list from given resource.
   *
   * @param <R> generic type for result item.
   * @return empty result list.
   */
  public static <R> ResultList<R> asSinglePage(List<R> result) {
    return new ResultList<>(result.size(), result);
  }

  @SafeVarargs
  public static <R> ResultList<R> asSinglePage(R... records) {
    return new ResultList<>(records.length, Arrays.asList(records));
  }
}
