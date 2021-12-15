package org.folio.innreach.controller.exception;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.folio.innreach.controller.d2ir")
public class HeadersAdvice {
  @ModelAttribute
  public void fetchHeader(@RequestHeader("X-To-Code") String xToCode,
                          @RequestHeader("X-From-Code") String xFromCode,
                          @RequestHeader("X-Request-Creation-Time") String xRequestCreationTime) {
    //will throw an exception if required headers are not present
  }

}
