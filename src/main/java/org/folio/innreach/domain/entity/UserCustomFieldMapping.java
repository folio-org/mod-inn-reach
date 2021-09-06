package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_custom_field_mapping")
@ToString(exclude = {"centralServer"})
public class UserCustomFieldMapping extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "custom_field_id")
  private UUID customFieldId;

  @Column(name = "custom_field_value")
  private String customFieldValue;

  @Column(name = "agencyCode")
  private String agencyCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
