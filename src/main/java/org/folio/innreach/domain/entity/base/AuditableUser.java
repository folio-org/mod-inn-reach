package org.folio.innreach.domain.entity.base;

import java.util.UUID;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AuditableUser {

  public static final AuditableUser SYSTEM = new AuditableUser(UUID.fromString("00000000-0000-0000-0000-000000000000"),
      "SYSTEM");

  private UUID id;
  private String name;

}