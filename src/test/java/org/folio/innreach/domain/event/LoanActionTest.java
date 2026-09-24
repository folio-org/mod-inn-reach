package org.folio.innreach.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class LoanActionTest {

  @ParameterizedTest
  @MethodSource("knownActionValues")
  void from_positive_returnsMatchingLoanActionCaseInsensitively(String actionValue, LoanAction expected) {
    assertThat(LoanAction.from(actionValue)).isEqualTo(expected);
    assertThat(LoanAction.from(actionValue.toUpperCase())).isEqualTo(expected);
  }

  @Test
  void from_negative_unknownActionReturnsNull() {
    assertThat(LoanAction.from("checkedout")).isNull();
    assertThat(LoanAction.from("bogus-action")).isNull();
  }

  @Test
  void from_negative_nullActionReturnsNull() {
    assertThat(LoanAction.from(null)).isNull();
  }

  @ParameterizedTest
  @MethodSource("knownActionValues")
  void getValue_positive_returnsUnderlyingActionString(String actionValue, LoanAction loanAction) {
    assertThat(loanAction.getValue()).isEqualTo(actionValue);
  }

  private static Stream<Arguments> knownActionValues() {
    return Stream.of(
      Arguments.of("checkedin", LoanAction.CHECKED_IN),
      Arguments.of("renewed", LoanAction.RENEW),
      Arguments.of("claimedReturned", LoanAction.CLAIMED_RETURNED),
      Arguments.of("recallrequested", LoanAction.RECALL_REQUESTED),
      Arguments.of("dueDateChanged", LoanAction.DUE_DATE_CHANGED)
    );
  }
}
