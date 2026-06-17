package org.folio.innreach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;

class DateHelperTest {

  @Test
  void toInstantTruncatedToSec_returnsInstantWithSecondPrecision() {
    var date = new Date(1_700_000_000_999L);

    var result = DateHelper.toInstantTruncatedToSec(date);

    assertEquals(Instant.ofEpochSecond(1_700_000_000L), result);
  }

  @Test
  void toInstantTruncatedToSec_dateWithExactSecond_returnsUnchangedInstant() {
    var date = new Date(1_700_000_000_000L);

    var result = DateHelper.toInstantTruncatedToSec(date);

    assertEquals(Instant.ofEpochSecond(1_700_000_000L), result);
  }

  @Test
  void toEpochSec_date_returnsEpochSecondsWithMillisTruncated() {
    var date = new Date(1_700_000_000_999L);

    var result = DateHelper.toEpochSec(date);

    assertEquals(1_700_000_000, result);
  }

  @Test
  void toEpochSec_date_epochZero_returnsZero() {
    var date = new Date(0L);

    var result = DateHelper.toEpochSec(date);

    assertEquals(0, result);
  }

  @Test
  void toEpochSec_instant_returnsEpochSecondsWithSubSecondsTruncated() {
    var instant = Instant.ofEpochSecond(1_700_000_000L, 999_999_999L);

    var result = DateHelper.toEpochSec(instant);

    assertEquals(1_700_000_000, result);
  }

  @Test
  void toEpochSec_instant_exactSecond_returnsUnchangedEpochSeconds() {
    var instant = Instant.ofEpochSecond(1_700_000_000L);

    var result = DateHelper.toEpochSec(instant);

    assertEquals(1_700_000_000, result);
  }

  @Test
  void toEpochSec_instant_epochZero_returnsZero() {
    var result = DateHelper.toEpochSec(Instant.EPOCH);

    assertEquals(0, result);
  }

  @Test
  void toEpochSec_dateAndInstant_samePointInTime_returnSameValue() {
    long epochMillis = 1_700_000_000_500L;
    var date = new Date(epochMillis);
    var instant = Instant.ofEpochMilli(epochMillis);

    assertEquals(DateHelper.toEpochSec(date), DateHelper.toEpochSec(instant));
  }

  @Test
  void addYearToDate_positiveN_returnsDateInFuture() {
    var before = new Date();

    var result = DateHelper.addYearToDate(1);

    assertTrue(result.after(before));
  }

  @Test
  void addYearToDate_negativeN_returnsDateInPast() {
    var before = new Date();

    var result = DateHelper.addYearToDate(-1);

    assertTrue(result.before(before));
  }

  @Test
  void addYearToDate_zero_returnsApproximatelyNow() {
    var before = new Date();

    var result = DateHelper.addYearToDate(0);

    var after = new Date();
    assertTrue(!result.before(before) && !result.after(after));
  }

  @Test
  void addYearToDate_multipleYears_addsCorrectNumberOfYears() {
    var result = DateHelper.addYearToDate(3);

    var threeYearsFromNow = Date.from(Instant.now().plus(3 * 365, ChronoUnit.DAYS));
    var approxDiff = Math.abs(result.getTime() - threeYearsFromNow.getTime());
    assertTrue(approxDiff < 2 * 24 * 60 * 60 * 1000L);
  }
}

