package org.folio.innreach.controller.d2ir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

@UtilityClass
@Log4j2
public class CirculationResultUtils {

  static ResultMatcher reasonMatch(Matcher<String> matcher) {
    return jsonPath("$.reason").value(matcher);
  }

  static ResultMatcher reasonMatch(Matcher<String> first, Matcher<String> second) {
    return jsonPath("$.reason").value(allOf(first, second));
  }

  @SafeVarargs
  static ResultMatcher reasonMatch(Matcher<String>... reasonMatchers) {
    return jsonPath("$.reason").value(allOf(reasonMatchers));
  }

  static ResultMatcher emptyErrors() {
    return jsonPath("$.errors").isEmpty();
  }

  static ResultMatcher failedStatus() {
    return jsonPath("$.status").value("failed");
  }

  static ResultMatcher failedWithReason(Matcher<String>... reasonMatchers) {
    return ResultMatcher.matchAll(failedStatus(), reasonMatch(reasonMatchers));
  }

  static ResultMatcher failedWithReason(String reason) {
    return ResultMatcher.matchAll(failedStatus(), reasonMatch(equalTo(reason)));
  }

  static ResultHandler logResponse() {
    return result -> log.info(result.getResponse().getContentAsString());
  }

  static <T> ResultMatcher exceptionMatch(Class<T> type) {
    return result -> assertThat(result.getResolvedException(), instanceOf(type));
  }

}
