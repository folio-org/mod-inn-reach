package org.folio.innreach.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public class DateHelper {

  public static Instant toInstantTruncatedToSec(Date date) {
    return date.toInstant().truncatedTo(ChronoUnit.SECONDS);
  }

  public static int toEpochSec(Date date) {
    return (int) toInstantTruncatedToSec(date).getEpochSecond();
  }

  public static Date addYearToDate(int n) {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    c.add(Calendar.YEAR, n);
    return c.getTime();
  }

}
