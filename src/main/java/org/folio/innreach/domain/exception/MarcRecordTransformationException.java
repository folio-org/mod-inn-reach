package org.folio.innreach.domain.exception;

public class MarcRecordTransformationException extends RuntimeException {

  public MarcRecordTransformationException(String message) {
    super(message);
  }

  public MarcRecordTransformationException(Throwable cause) {
    super(cause);
  }
}
